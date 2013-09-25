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

import org.tc65sh.util.ByteArray;

import java.util.Calendar;
import java.util.Date;

public abstract class Obex {
	
	/* OBEX constants see OBEX specification 2.2 */
	public static final byte HEADER_COUNT = (byte) 0xC0;
	public static final byte HEADER_NAME = (byte) 0x01;
	public static final byte HEADER_TYPE = (byte) 0x42;
	public static final byte HEADER_LENGTH = (byte) 0xC3;
	public static final byte HEADER_TIME = (byte) 0x44;
	public static final byte HEADER_DESCRIPTION = (byte) 0x05;
	public static final byte HEADER_TARGET = (byte) 0x46;
	public static final byte HEADER_HTTP = (byte) 0x47;
	public static final byte HEADER_BODY = (byte) 0x48;
	public static final byte HEADER_END_OF_BODY = (byte) 0x49;
	public static final byte HEADER_WHO = (byte) 0x4A;
	public static final byte HEADER_CONNECTION_ID = (byte) 0xCB;
	public static final byte HEADER_APP_PARAMETERS = (byte) 0x4C;
	public static final byte HEADER_AUTH_CHALLENGE = (byte) 0x4D;
	public static final byte HEADER_AUTH_RESPONSE = (byte) 0x4E;
	public static final byte HEADER_CREATOR_ID = (byte) 0xCF;
	public static final byte HEADER_WAN_UUID = (byte) 0x50;
	public static final byte HEADER_OBJECT_CLASS = (byte) 0x51;
	public static final byte HEADER_SESSION_PARAMETERS = (byte) 0x52;
	public static final byte HEADER_SESSION_SEQUENCE_NUMBER = (byte) 0x93;

	public static final byte REQUEST_CONNECT = (byte)0x80;
    public static final byte REQUEST_DISCONNECT = (byte)0x81;
    public static final byte REQUEST_PUT = (byte)0x02;
    public static final byte REQUEST_GET = (byte)0x03;
    public static final byte REQUEST_SETPATH = (byte)0x85;
    public static final byte REQUEST_SETPATH2 = (byte)0x86;
    public static final byte REQUEST_SESSION = (byte)0x87;
    public static final byte REQUEST_ABORT = (byte)0xFF;    
    public static final byte REQUEST_FINAL = (byte)0x80;

    public static final byte FLAG_SETPATH_CREATE = (byte)0x00;
    public static final byte FLAG_SETPATH_NOCREATE = (byte)0x02;
    public static final byte FLAG_SETPATH_PARENT_FOLDER = (byte)0x03;

    public static final byte RESPONSE_SUCCESS = (byte)0x20;
    public static final byte RESPONSE_CONTINUE = (byte)0x10;
    public static final byte RESPONSE_CREATED = (byte)0x21;
    public static final byte RESPONSE_BADREQUEST = (byte)0x40;
    public static final byte RESPONSE_FINAL = (byte)0x80;
    
	public static ByteArray encodeUtf16String(String str) {
		ByteArray b = new ByteArray();
		for( int i=0 ; i<str.length() ; i++ ) {
			b.append(0x00);
			b.append(str.charAt(i));
		}
		return b;
	}
    
	public static String encodeXmlDateTime(Date d) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH)+1;
		int day = cal.get(Calendar.DAY_OF_MONTH);
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);
		int second = cal.get(Calendar.SECOND);
		StringBuilder sb = new StringBuilder();
		sb.append(padWith0(year,4));
		sb.append(padWith0(month,2));
		sb.append(padWith0(day,2));
		sb.append('T');
		sb.append(padWith0(hour,2));
		sb.append(padWith0(minute,2));
		sb.append(padWith0(second,2));
		return sb.toString();
	}
    
	public static ByteArray encodeDateTime(Date d) {
		String s = encodeXmlDateTime(d);
		ByteArray b = new ByteArray(s,"ISO-8859-1");
		return b;
	}
	
	public static Date decodeXmlDateTime(String s) {
		if ( s.length() < 15 ) throw new RuntimeException("invalid XML time '"+s+"'");
		// 0   4 6  9 1 3
		// 20020101T001650
		String syear = s.substring(0,4);
		String smonth = s.substring(4,6);
		String sday = s.substring(6,8);
		String shour = s.substring(9,11);
		String sminute = s.substring(11,13);
		String ssecond = s.substring(13);
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(0);
		cal.set(Calendar.YEAR, Integer.parseInt(syear));
		cal.set(Calendar.MONTH, Integer.parseInt(smonth)-1);
		cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(sday));
		cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(shour));
		cal.set(Calendar.MINUTE, Integer.parseInt(sminute));
		cal.set(Calendar.SECOND, Integer.parseInt(ssecond));
		return cal.getTime();
	}
    
	private static String padWith0(int value, int minLength) {
		String s = ""+value;
		while( s.length() < minLength ) {
			s = "0"+s;
		}
		return s;
	}
	
	public static byte[] shortToBytes(int i) {
		byte[] b = new byte[2];
		b[0] = (byte) ( i>>8 & 0xFF );
		b[1] = (byte) ( i & 0xFF );
		return b;
	}

	public static byte[] intToBytes(int i) {
		byte[] b = new byte[4];
		b[0] = (byte) ( i>>24 & 0xFF );
		b[1] = (byte) ( i>>16 & 0xFF );
		b[2] = (byte) ( i>>8 & 0xFF );
		b[3] = (byte) ( i & 0xFF );
		return b;
	}

	public static int bytesToInt(byte[] bytes) {
        int result = 0;
        for (int i = 0; i < bytes.length; i++) {
            result = result << 8;
            int temp = (int) bytes[i];
            if (temp < 0) {
                temp += 0x100;
            }
            result += temp;
        }
        return result;
    }
	
	public static int bytesToShort(byte b1, byte b2) {
		return bytesToInt(new byte[] {b1,b2});
    }
}
