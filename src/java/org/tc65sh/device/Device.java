// This file is part of TC65SH.
// 
// TC65SH is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
// 
// TC65SH is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public License
// along with TC65SH. If not, see <http://www.gnu.org/licenses/>.
// 
package org.tc65sh.device;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.tc65sh.util.ByteArray;
import org.tc65sh.util.Log;

public class Device {
	
	public static final long DEFAULT_SLEEP_MILLIS = 10;
	public static final long DEFAULT_INTERNAL_AT_RESPONSE_TIMEOUT = 5000;
	public static final long DEFAULT_AT_COMMAND_RESPONSE_TIMEOUT = 15000;
	public static final long DEFAULT_OBEX_RESPONSE_TIMEOUT = 5000;
	public static final long DEFAULT_ERASE_DISK_TIMEOUT = 20000;
	
	public static final char FLOWCONTROL_NONE = 'n';
	public static final char FLOWCONTROL_RTSCTS = 'r';
	public static final char FLOWCONTROL_XONXOFF = 'x';
	
	private SerialPort serialPort;
	private InputStream serialIn;
	private OutputStream serialOut;
	private boolean inObexMode = false;

	public void connect(String portname, int baudrate, char flowControl) throws Exception {
		Log.debug(this.getClass(), "connecting device "+portname+", "+baudrate+" baud");
        boolean isCommonPortname = portname.contains("ttyS") || portname.contains("COM");
		if ( ! isCommonPortname ) {
            System.setProperty("gnu.io.rxtx.SerialPorts", portname);
        }
		System.setProperty("gnu.io.rxtx.NoVersionOutput", "true");
		CommPortIdentifier commPortIdentifier = CommPortIdentifier.getPortIdentifier(portname);
		CommPort commPort = commPortIdentifier.open("tc65sh", 2000);
		serialPort = (SerialPort) commPort;
		serialPort.setSerialPortParams(baudrate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
		serialPort.enableReceiveTimeout(2000);
		if ( flowControl == FLOWCONTROL_NONE ) {
			serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
		} else if ( flowControl == FLOWCONTROL_RTSCTS) {
			serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_OUT | SerialPort.FLOWCONTROL_RTSCTS_IN);
		} else if ( flowControl == FLOWCONTROL_XONXOFF) {
			serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_XONXOFF_OUT | SerialPort.FLOWCONTROL_XONXOFF_IN);
		} else {
			throw new RuntimeException("invalid flowControl "+flowControl);
		}
		serialIn = serialPort.getInputStream();
		serialOut = serialPort.getOutputStream();
	}
	
	public void initDevice() throws IOException {
		Log.debug(this.getClass(), "initializing device connection");
		sendByteArray(new ByteArray("AT\r","ISO-8859-1"));
		waitForATResponseWithOK(DEFAULT_INTERNAL_AT_RESPONSE_TIMEOUT, true);
		sendByteArray(new ByteArray("ATE\r","ISO-8859-1"));
		waitForATResponseWithOK(DEFAULT_INTERNAL_AT_RESPONSE_TIMEOUT, true);
		sendByteArray(new ByteArray("AT\r","ISO-8859-1"));
		waitForATResponseWithOK(DEFAULT_INTERNAL_AT_RESPONSE_TIMEOUT, true);
		sendByteArray(new ByteArray("AT\r","ISO-8859-1"));
		waitForATResponseWithOK(DEFAULT_INTERNAL_AT_RESPONSE_TIMEOUT, true);
		sendByteArray(new ByteArray("ATI\r","ISO-8859-1"));
		ByteArray response = waitForATResponseWithOK(DEFAULT_INTERNAL_AT_RESPONSE_TIMEOUT, true);
		Log.debug(this.getClass(), "" + response.toPrintableString());
	}
	
	public void waitForSysstart(long timeout) throws IOException {
		Log.debug(getClass(), "waiting for sysstart");
		boolean foundSysstart = false;
		long t1 = System.currentTimeMillis();
		ByteArray byteArray = new ByteArray();
		while( ! foundSysstart ) {
			int readCount = receiveIntoByteArray(byteArray);
			if ( readCount > 0 ) {
				if ( byteArray.toPrintableString().contains("^SYSSTART") ) {
					foundSysstart = true;
				}
			} else {
				sleep(100);
			}
			long runtime = System.currentTimeMillis() - t1;
			if ( runtime > timeout ) {
				throw new IOException("timeout waiting for SYSSTART after "+runtime+" ms");
			}
		}
	}
	
	
	public void disconnect() throws IOException, InterruptedException {
		if ( inObexMode ) {
			closeObexMode();
		}
		Log.debug(this.getClass(), "disconnecting device");
		
		serialIn.close();
		serialOut.close();
		serialPort.close();
		
		serialIn = null;
		serialOut = null;
		serialPort = null;
	}

	public void obexOpenObexMode() throws IOException {
		openObexMode();
	}

	public List<FileInfo> obexGetFolderListing() throws IOException {
		if ( ! inObexMode ) {
			openObexMode();
		}
		String typeUid = "x-obex/folder-listing";
		ByteArray typeHeader = new ByteArray();
		typeHeader.append(Obex.HEADER_TYPE);
		typeHeader.append(Obex.shortToBytes(3+typeUid.length()));
		typeHeader.append(typeUid, "ISO-8859-1");
		ByteArray req = new ByteArray();
		req.append(Obex.REQUEST_GET | Obex.REQUEST_FINAL);
		req.append(Obex.shortToBytes(3+typeHeader.length()));
		req.append(typeHeader);
		sendByteArray(req);
		ByteArray body = receiveBodyToEnd();
		Log.debug(this.getClass(), new String(body.getBuffer(), "ISO-8859-1"));
		String xml = new String(body.getBuffer(), "ISO-8859-1");
		String[] lines = xml.split("\n"); 
		ArrayList<FileInfo> fileInfos = new ArrayList<FileInfo>(); 
		for( String line : lines ) {
			line = line.trim();
			FileInfo fi = parseFolderListingLine(line);
			if ( fi != null ) {
				fileInfos.add(fi);
			}
		}
		Collections.sort(fileInfos);
		return fileInfos;
	}


	private FileInfo parseFolderListingLine(String line) {
		FileInfo result = null;
		if ( line.startsWith("<file ") ) {
			result = new FileInfo(false,null,-1,null);
		} else if ( line.startsWith("<folder ") ) {
			result = new FileInfo(true,null,-1,null);
		}		
		if ( result != null ) {
			result.name = extractXmlTagValue(line, "name");
			String xmldate = extractXmlTagValue(line, "modified");
			if ( xmldate != null ) {
				result.date = Obex.decodeXmlDateTime(xmldate);
			}
			String ssize = extractXmlTagValue(line, "size");
			if ( ssize != null ) {
				result.filesize = Integer.parseInt(ssize);
			}
		}
		return result;
	}

	private String extractXmlTagValue(String line, String tag) {
		int i = line.indexOf(tag+"=\"");
		if ( i < 0 ) return null;
		i += 2 + tag.length();
		int j = line.indexOf('\"', i);
		if ( j < 0 ) return null;
		String value = line.substring(i, j);
		return value;
	}

	public void obexDeleteFile(String filename) throws IOException {
		if ( ! inObexMode ) {
			openObexMode();
		}
		ByteArray obexFilename = Obex.encodeUtf16String(filename);
		ByteArray nameHeader = new ByteArray();
		nameHeader.append(Obex.HEADER_NAME);
		nameHeader.append(Obex.shortToBytes(3+obexFilename.length()));
		nameHeader.append(obexFilename);
		ByteArray req = new ByteArray();
		req.append(Obex.REQUEST_PUT | Obex.REQUEST_FINAL);
		req.append(Obex.shortToBytes(3+nameHeader.length()));
		req.append(nameHeader);
		sendByteArray(req);
		ByteArray response = receiveObexResponse(DEFAULT_OBEX_RESPONSE_TIMEOUT);
		validateResponseCode(response);
	}
	

	public void obexEraseDisk() throws IOException {
		if ( ! inObexMode ) {
			openObexMode();
		}
		ByteArray appParamsHeader = new ByteArray();
		appParamsHeader.append(Obex.HEADER_APP_PARAMETERS);
		appParamsHeader.append(Obex.shortToBytes(5));
		appParamsHeader.append(0x31);
		appParamsHeader.append(0x00);
		ByteArray req = new ByteArray();
		req.append(Obex.REQUEST_PUT | Obex.REQUEST_FINAL);
		req.append(Obex.shortToBytes(3+appParamsHeader.length()));
		req.append(appParamsHeader);
		sendByteArray(req);
		ByteArray response = receiveObexResponse(DEFAULT_ERASE_DISK_TIMEOUT); // may take a long time
		validateResponseCode(response);
	}
	

	public FileHolder obexGetFile(String filename) throws IOException {
		if ( ! inObexMode ) {
			openObexMode();
		}
		ByteArray obexFilename = Obex.encodeUtf16String(filename);
		ByteArray header = new ByteArray();
		header.append(Obex.HEADER_NAME);		
		header.append(Obex.shortToBytes(3+obexFilename.length()));
		header.append(obexFilename);
		ByteArray req = new ByteArray();
		req.append(Obex.REQUEST_GET | Obex.REQUEST_FINAL);  
		req.append(Obex.shortToBytes(3+header.length()));
		req.append(header);
		sendByteArray(req);
		ByteArray body = receiveBodyToEnd();
		FileHolder result = new FileHolder(new FileInfo(false, filename, body.length(), null), body);
		return result;
	}
	
	public void obexPutFile(FileHolder file) throws IOException {
		if ( ! inObexMode ) {
			openObexMode();
		}
		int maxPartLength = 512;
		int writeCount = 0;
		long t1 = System.currentTimeMillis();
		while( writeCount < file.content.length()) {
			int contentPartLength = file.content.length() - writeCount;
			boolean isLastPart;
			boolean isFirstPart = writeCount == 0;
			if ( contentPartLength > maxPartLength ) {
				contentPartLength = maxPartLength;
				isLastPart = false;
			} else {
				isLastPart = true;
			}
			putFilePart(file, writeCount, contentPartLength, isFirstPart, isLastPart);
			writeCount += contentPartLength;
		}
		long t2 = System.currentTimeMillis();
		Log.debug(getClass(), "sent "+file.content.length()+" bytes, "+(t2-t1)+" ms");
	}
	
	
	public void obexMakeDir(String pathname) throws IOException {
		if ( ! inObexMode ) {
			openObexMode();
		}
		setPath(pathname,true);
		setPath("..",false);
	}
	
	public void obexChangeDir(String pathname) throws IOException {
		if ( ! inObexMode ) {
			openObexMode();
		}
		setPath(pathname,false);
	}
	
	public String executeAtCommand(String atCommand) throws IOException {
		if ( inObexMode ) {
			closeObexMode();
		}
		ByteArray request = new ByteArray(atCommand,"ISO-8859-1");
		request.append(13);
		sendByteArray(request);
		ByteArray response = waitForATResponseWithOK(DEFAULT_AT_COMMAND_RESPONSE_TIMEOUT, true);
		return new String(response.getBuffer(),"ISO-8859-1");
	}
	
	
	
	// private stuff

	private void openObexMode() throws IOException {
		Log.debug(this.getClass(), "opening obex mode");
		sendByteArray(new ByteArray("AT\\Q3\r","ISO-8859-1"));
		waitForATResponseWithOK(DEFAULT_INTERNAL_AT_RESPONSE_TIMEOUT, true);
		sendByteArray(new ByteArray("AT^SQWE=0\r","ISO-8859-1"));
		waitForATResponseWithOK(DEFAULT_INTERNAL_AT_RESPONSE_TIMEOUT, true);
		sendByteArray(new ByteArray("AT^SQWE=3\r","ISO-8859-1"));
		waitForATResponseWithOK(DEFAULT_INTERNAL_AT_RESPONSE_TIMEOUT, true);
		Log.debug(this.getClass(), "connecting obex");
		ByteArray fsUid = new ByteArray(new byte[] { (byte) 0x6b, (byte) 0x01, (byte) 0xcb, (byte) 0x31, (byte) 0x41, (byte) 0x06, (byte) 0x11, (byte) 0xd4, (byte) 0x9a, (byte) 0x77, (byte) 0x00, (byte) 0x50, (byte) 0xda, (byte) 0x3f, (byte) 0x47, (byte) 0x1f });
		ByteArray targetHeader = new ByteArray();
		targetHeader.append(Obex.HEADER_TARGET);
		targetHeader.append(Obex.shortToBytes(3+fsUid.length()));
		targetHeader.append(fsUid);
		ByteArray req = new ByteArray();
		req.append(Obex.REQUEST_CONNECT);
		req.append(Obex.shortToBytes(7+targetHeader.length()));
		req.append(0x13); // obex version
		req.append(0x00); // flags
		req.append(Obex.shortToBytes(0xffff)); // max packet length
		req.append(targetHeader);
		sendByteArray(req);
		ByteArray response = receiveObexResponse(DEFAULT_OBEX_RESPONSE_TIMEOUT);
		validateResponseCode(response);
		inObexMode = true;
	}

	
	private void closeObexMode() throws IOException {
		Log.debug(this.getClass(), "closing obex mode obex");
		ByteArray req = new ByteArray();
		req.append(Obex.REQUEST_DISCONNECT);
		req.append(Obex.shortToBytes(3));
		sendByteArray(req);
		ByteArray response = receiveObexResponse(DEFAULT_OBEX_RESPONSE_TIMEOUT);
		validateResponseCode(response);
		Log.debug(this.getClass(), "escaping data mode");
		boolean foundOK = false;
		int max = 5;
		while( ! foundOK && max >= 0 ) {
			max--;
			sendByteArray(new ByteArray("+++","ISO-8859-1"));
			ByteArray plusResponse = waitForATResponseWithOK(1000, false);				
			if ( plusResponse != null ) {
				foundOK = true;
			}
		}
		sendByteArray(new ByteArray("ATE1\r","ISO-8859-1"));
		waitForATResponseWithOK(DEFAULT_INTERNAL_AT_RESPONSE_TIMEOUT, true);
		inObexMode = false;
	}
	
	
	private void putFilePart(FileHolder file, int contentPartOffset, int contentPartLength, boolean isFirstPart, boolean isLastPart) throws IOException {
		Log.debug(getClass(), "sending bytes "+contentPartOffset+".."+(contentPartOffset+contentPartLength)+" to "+file.fileInfo.name);
		ByteArray contentPart = file.content.subArray(contentPartOffset, contentPartLength);
		ByteArray nameHeader = new ByteArray();
		ByteArray lengthHeader = new ByteArray();
		ByteArray timeHeader = new ByteArray();
		if ( isFirstPart ) {
			ByteArray obexFilename = Obex.encodeUtf16String(file.fileInfo.name);
			// HEADER_NAME
			nameHeader.append(Obex.HEADER_NAME);		
			nameHeader.append(Obex.shortToBytes(3+obexFilename.length()));
			nameHeader.append(obexFilename);
			// HEADER_LENGTH
			lengthHeader.append(Obex.HEADER_LENGTH);		
			lengthHeader.append(Obex.intToBytes(file.fileInfo.filesize));
			// HEADER_TIME
			ByteArray obexTime = Obex.encodeDateTime(file.fileInfo.date);
			timeHeader.append(Obex.HEADER_TIME);		
			timeHeader.append(Obex.shortToBytes(3+obexTime.length()));
			timeHeader.append(obexTime);
		}
		// HEADER_BODY
		ByteArray bodyHeader = new ByteArray();
		if ( isLastPart ) {
			bodyHeader.append(Obex.HEADER_END_OF_BODY);		
		} else {
			bodyHeader.append(Obex.HEADER_BODY);		
		}
		bodyHeader.append(Obex.shortToBytes(3+contentPart.length()));
		bodyHeader.append(contentPart);
		// REQUEST_PUT
		ByteArray req = new ByteArray();
		if ( isLastPart ) {
			req.append(Obex.REQUEST_PUT | Obex.REQUEST_FINAL );
		} else {
			req.append(Obex.REQUEST_PUT);
		}
		req.append(Obex.shortToBytes(3+nameHeader.length()+lengthHeader.length()+timeHeader.length()+bodyHeader.length()));
		req.append(nameHeader);
		req.append(lengthHeader);
		req.append(timeHeader);
		req.append(bodyHeader);
		sendByteArray(req);
		ByteArray response = receiveObexResponse(DEFAULT_OBEX_RESPONSE_TIMEOUT);
		validateResponseCode(response);
	}
	
	
	private ByteArray receiveBodyToEnd() throws IOException {
		ByteArray response = receiveObexResponse(DEFAULT_OBEX_RESPONSE_TIMEOUT);
		validateResponseCode(response);
		ByteArray fullBody = new ByteArray();
		ByteArray bodyPart = extractGetResponseBody(response); 
		if ( bodyPart != null ) {
			fullBody.append(bodyPart);
		}
		while( isObexContinue(response) ) {
			ByteArray req = new ByteArray();
			req.append(Obex.REQUEST_GET);
			req.append(Obex.shortToBytes(3));
			sendByteArray(req);
			response = receiveObexResponse(DEFAULT_OBEX_RESPONSE_TIMEOUT);
			validateResponseCode(response);
			bodyPart = extractGetResponseBody(response); 
			if ( bodyPart != null ) {
				fullBody.append(bodyPart);
			}
		}
		return fullBody;
	}

	
	

	
	private ByteArray waitForATResponseWithOK(long timeoutmillis, boolean responseRequired) throws IOException {
		ByteArray response = new ByteArray();
		long t1 = System.currentTimeMillis();
		boolean foundOK = false;
		while (!foundOK) {
			receiveIntoByteArray(response);
			if ( response.toPrintableString().contains("OK") ) {
				foundOK = true;
			} else {
				long runtime = System.currentTimeMillis() - t1; 
				if ( runtime > timeoutmillis) {
					if ( responseRequired ) {
						throw new IOException("response timeout waiting for OK after "+runtime+" ms and "+response.length()+" bytes");
					} else {
						return null;
					}
				}
				sleep(DEFAULT_SLEEP_MILLIS);
			}
		}
		return response;
	}

	
	
	private void setPath(String pathname, boolean create) throws IOException {
		ByteArray header = new ByteArray();
		byte flags;
		if ( pathname == null || pathname.length() == 0 || pathname.equals("/") || pathname.equals("..") || pathname.toLowerCase().equals("a:") || pathname.toLowerCase().equals("a:/") ) {
			flags = Obex.FLAG_SETPATH_PARENT_FOLDER;
		} else {
			ByteArray obexPath = Obex.encodeUtf16String(pathname);
			Log.debug(this.getClass(), "obexPath = "+obexPath.toPrintableString()+" " +obexPath.toHexString());
			header.append(Obex.HEADER_NAME);
			header.append(Obex.shortToBytes(obexPath.length()+3));
			header.append(obexPath);
			if ( create ) {
				flags = Obex.FLAG_SETPATH_CREATE;
			} else {
				flags = Obex.FLAG_SETPATH_NOCREATE;
			}
		}
		ByteArray req = new ByteArray();
		req.append(Obex.REQUEST_SETPATH);
		req.append(Obex.shortToBytes(header.length()+5));
		req.append(flags);
		req.append(0x00);
		req.append(header);
		sendByteArray(req);
		ByteArray response = receiveObexResponse(DEFAULT_OBEX_RESPONSE_TIMEOUT);
		validateResponseCode(response);
	}

	
	private ByteArray extractGetResponseBody(ByteArray response) {
		ByteArray body = null;
		int i = indexOfBodyHeader(response);
		if ( i >= 0 ) {
			Log.debug(getClass(), "indexOfBodyHeader = "+i);
			body = response.subArray(i+3, response.length()-i-3);
		} else {
			Log.debug(getClass(), "no BodyHeader found in response");
		}
		return body;
	}

	private int indexOfBodyHeader(ByteArray response) {
		int result = -1;
		int i=3;
		byte[] buf = response.getBuffer();
		while( result == -1 && i < buf.length ) {
			if ( buf[i] == Obex.HEADER_BODY ) {
				result = i;
			} else if ( buf[i] == Obex.HEADER_LENGTH ) {
					i += 5;
			} else {
				int hdrLen = Obex.bytesToShort(buf[i+1],buf[i+2]);
				i += hdrLen;
			}
		}
		return result;
	}

	private boolean isObexContinue(ByteArray response) {
		int code = response.getBuffer()[0]; 
		return  (code & (int)Obex.RESPONSE_CONTINUE) == (int)Obex.RESPONSE_CONTINUE;
	}

	private void validateResponseCode(ByteArray response) throws IOException {		
		int code = response.getBuffer()[0]; 
		if ( (code & (int)Obex.RESPONSE_SUCCESS) == (int)Obex.RESPONSE_SUCCESS ) return;
		if ( (code & (int)Obex.RESPONSE_CONTINUE) == (int)Obex.RESPONSE_CONTINUE) return;
		if ( (code & (int)Obex.RESPONSE_CREATED) == (int)Obex.RESPONSE_CREATED) return;
		throw new IOException("response validation error, code="+code);
	}

	private ByteArray receiveObexResponse(long timeoutMillis) throws IOException {
		ByteArray response = new ByteArray();
		long t1 = System.currentTimeMillis();
		boolean complete = false;
		while( ! complete ) {
			receiveIntoByteArray(response);
			if ( response.length() >= 3 ) {
				int expectedFrameLength = Obex.bytesToInt(response.subArray(1,2).getBuffer());
				if ( response.length() >= expectedFrameLength ) {
					complete = true;
				}
			}
			if ( ! complete ) {
				long runtime = System.currentTimeMillis()-t1; 
				if ( runtime > timeoutMillis ) {
					throw new IOException("obex response timeout after "+runtime+" ms and "+response.length()+" bytes");
				}
			}
		}
		return response;
	}

	private void sendByteArray(ByteArray byteArray) throws IOException {
		if ( Log.isDebugEnabled() ) {
			Log.debug(this.getClass(), "send " + byteArray.length() + " bytes: " + byteArray.toHexString() + byteArray.toPrintableString());
		}
		byte[] buffer = byteArray.getBuffer();
		serialOut.write(buffer);
		serialOut.flush();
	}
	
	private int receiveIntoByteArray(ByteArray byteArray) throws IOException {
		if ( serialIn.available() == 0 ) {
			sleep(DEFAULT_SLEEP_MILLIS);
		}
		byte[] buf = new byte[512];
		int totalReadCount = 0;
		while( serialIn.available() > 0 ) {
			int readCount = serialIn.read(buf);
			totalReadCount += readCount;
			ByteArray temp = new ByteArray(buf, 0, readCount);
			if ( Log.isDebugEnabled() ) {
				Log.debug(this.getClass(), "received " + temp.length() + " bytes: " + temp.toHexString() + temp.toPrintableString());
			}
			byteArray.append(temp);
		}
		return totalReadCount;
	}
	
	private void sleep( long millis ) {
		try {
			Thread.sleep(millis);
		} catch (Exception e) {
			// ignore
		}
	}
}
