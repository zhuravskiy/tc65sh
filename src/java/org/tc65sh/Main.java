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
package org.tc65sh;

import org.tc65sh.device.Device;
import org.tc65sh.device.FileHolder;
import org.tc65sh.device.FileInfo;
import org.tc65sh.ui.*;
import org.tc65sh.util.ByteArray;
import org.tc65sh.util.FileUtils;
import org.tc65sh.util.Log;
import org.tc65sh.util.StringSplitter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class Main {

	private static final String VERSION_STRING = "1.4.0";

	private static final String DEFAULT_PORTNAME = "COM1";
	private static final int DEFAULT_BAUDRATE = 115200;
	private static final char DEFAULT_FLOWCONTROL = Device.FLOWCONTROL_RTSCTS;

	public static void main(String[] args) {
		try {
			if (args.length == 0) {
				new Main().printUsage();
				System.exit(0);
			}
			for (String arg : args) {
				if ("--help".equals(arg) || "-h".equals(arg)) {
					new Main().printUsage();
					System.exit(0);
				}
			}
			new Main().exec(args);
			System.exit(0);
		} catch (Exception e) {
			Log.info("PANIC", e);
		}
	}

	private String portname = DEFAULT_PORTNAME;
	private int baudrate = DEFAULT_BAUDRATE;
	private char flowControl = DEFAULT_FLOWCONTROL;
	private int sysstartWaitSec = 0; // 0 means "do not wait for SYSSTART"

	public void exec(String[] args) throws Exception {
		Log.logLevel = Log.LOG_INFO;
		Log.info("tc65sh v" + VERSION_STRING);
		UserInterface ui = null;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-p") || args[i].equals("--portname")) {
				portname = args[i + 1];
			} else if (args[i].equals("-b") || args[i].equals("--baudrate")) {
				baudrate = Integer.parseInt(args[i + 1]);
			} else if (args[i].equals("-fc") || args[i].equals("--flowcontrol")) {
				flowControl = args[i + 1].charAt(0);
			} else if (args[i].equals("-d") || args[i].equals("--debug")) {
				Log.logLevel = Log.LOG_DEBUG;
			} else if (args[i].equals("-q") || args[i].equals("--quiet")) {
				Log.logLevel = Log.LOG_NONE;
			} else if (args[i].equals("-w") || args[i].equals("--wait")) {
				sysstartWaitSec = Integer.parseInt(args[i + 1]);
			} else if (args[i].equals("-f") || args[i].equals("--file")) {
				Log.info("processing command file " + args[i + 1]);
				ui = new CommandFileUserInterface(new File(args[i + 1]));
			} else if (args[i].equals("-c") || args[i].equals("--commands")) {
				Log.info("processing commands " + args[i + 1]);
				ui = new CommandLineUserInterface(args[i + 1]);
			} else if (args[i].equals("-t") || args[i].equals("--telnet")) {
				ui = new TelnetUserInterface(Integer.parseInt(args[i + 1]));
			}
		}

		Device device = new Device();
		try {
			Log.info("connecting " + portname + " with " + baudrate + " baud, flowControl " + flowControl);
			device.connect(portname, baudrate, flowControl);
			if (sysstartWaitSec > 0) {
				Log.info("waiting " + sysstartWaitSec + " sec for SYSSTART");
				device.waitForSysstart(sysstartWaitSec * 1000);
			}
			Log.info("initializing device");
			device.initDevice();
			Log.info("forcing obex mode");
			device.obexOpenObexMode();
			if (ui == null) {
				ui = new InteractiveUserInterface();
				ui.println("starting interactive mode, type 'exit' to exit, 'help' for help.");
			}
			try {
				doCommandLoop(device, ui);
			} catch (Exception e) {
				e.printStackTrace();
			}
			Log.info("disconnecting");
			device.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void doCommandLoop(Device device, UserInterface ui) throws Exception {
		String cmdline;
		while ((cmdline = ui.readCommand()) != null) {
			cmdline = cmdline.trim();
			ui.echoCommand(cmdline);
			String tok[] = StringSplitter.split(cmdline);
			if (cmdline.startsWith("#") || cmdline.startsWith("//")) {
				// do nothing, it's a comment
			} else if (cmdline.startsWith("AT") || cmdline.startsWith("at")) {
				String response = device.executeAtCommand(cmdline);
				ui.println(response);
			} else if (tok[0].equals("cd")) {
				// cd <directory>
				if (tok.length > 1) {
					if (FileUtils.isFileName(tok[1])) {
						device.obexChangeDir(tok[1]);
					} else {
						ui.println("Error: directory must not be a path!");
					}
				} else {
					ui.println("Error: no directory");
				}
			} else if (tok[0].equals("mkdir")) {
				// mkdir <directory>
				if (tok.length > 1) {
					if (FileUtils.isFileName(tok[1])) {
						device.obexMakeDir(tok[1]);
					} else {
						ui.println("Error: directory must not be a path!");
					}
				} else {
					ui.println("Error: no directory");
				}
			} else if (tok[0].equals("ls") || tok[0].equals("dir")) {
				// ls
				List<FileInfo> listing = device.obexGetFolderListing();
				printFolderListing(listing, ui);
			} else if (tok[0].equals("rm") || tok[0].equals("del")) {
				// rm <deviceFilename>
				if (tok.length > 1) {
					if (FileUtils.isFileName(tok[1])) {
						device.obexDeleteFile(tok[1]);
					} else {
						ui.println("Error: deviceFilename must not be a path!");
					}
				} else {
					ui.println("Error: no deviceFilename");
				}
			} else if (tok[0].equals("put")) {
				// put <localFilePath> <optional: deviceFilename>
				if (tok.length > 1) {
					FileHolder fileHolder = loadLocalFile(tok[1]);
					if (tok.length > 2) {
						fileHolder.fileInfo.name = tok[2];
					} else {
						fileHolder.fileInfo.name = FileUtils.extractFilename(tok[1]);
					}
					if (FileUtils.isFileName(fileHolder.fileInfo.name)) {
						device.obexDeleteFile(fileHolder.fileInfo.name);
						device.obexPutFile(fileHolder);
					} else {
						ui.println("Error: deviceFilename must not be a path!");
					}
				} else {
					ui.println("Error: no localFilePath");
				}
			} else if (tok[0].equals("get")) {
				// get <deviceFilename> <optional: localFilePath>
				if (tok.length > 1) {
					if (FileUtils.isFileName(tok[1])) {
						FileHolder fh = device.obexGetFile(tok[1]);
						if (tok.length > 2) {
							fh.fileInfo.name = tok[2];
						} else {
							fh.fileInfo.name = tok[1];
						}
						saveLocalFile(fh);
					} else {
						ui.println("Error: deviceFilename must not be a path!");
					}
				} else {
					ui.println("Error: no deviceFilename");
				}
			} else if (tok[0].equals("getd")) {
				// getd <deviceFilename> <optional: localFilePath>
				if (tok.length > 1) {
					if (FileUtils.isFileName(tok[1])) {
						if (tok.length > 2) {
							getDirectory(device, tok[1], tok[2]);
						} else {
							getDirectory(device, tok[1], tok[1]);
						}
					} else {
						ui.println("Error: deviceFilename must not be a path!");
					}
				} else {
					ui.println("Error: no deviceFilename");
				}
			} else if (tok[0].equals("cat")) {
				// cat <deviceFilename>
				if (tok.length > 1) {
					if (FileUtils.isFileName(tok[1])) {
						FileHolder fh = device.obexGetFile(tok[1]);
						String catString = new String(fh.content.getBuffer());
						ui.println(catString);
					} else {
						ui.println("Error: deviceFilename must not be a path!");
					}
				}
			} else if (tok[0].equals("erasedisk")) {
				// erasedisk
				device.obexEraseDisk();
			} else if (tok[0].equals("sleep")) {
				// sleep <milliseconds>
				if (tok.length > 1) {
					long millis = Long.parseLong(tok[1]);
					Thread.sleep(millis);
				}
			} else if (tok[0].equals("help")) {
				printHelp(ui);
			}
		}
	}


	private void getFilename(Device device, String deviceFilename, String localFilename) throws IOException {
		if (localFilename == null) {
			localFilename = deviceFilename;
		}

		FileHolder fh = device.obexGetFile(deviceFilename);
		fh.fileInfo.name = localFilename;

		saveLocalFile(fh);
	}

	private void getDirectory(Device device, String deviceFilename, String localDirname) throws IOException {

		List<FileInfo> listing = null;
		try {
			device.obexChangeDir(deviceFilename);
			listing = device.obexGetFolderListing();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		File dir = new File(localDirname);
		dir.mkdirs();

		for (FileInfo info : listing) {
			if (info.directory) {
				continue;
			}

			String localFilename = localDirname + File.separator + info.name;

			System.out.println("Copying " + info.name + " to " + localDirname);
			getFilename(device, info.name, localFilename);
		}

		for (FileInfo info : listing) {
			if (!info.directory) {
				continue;
			}

			String subDirname = info.name;
			String localSubDirname = localDirname + File.separatorChar + subDirname;
			getDirectory(device, subDirname, localSubDirname);
		}

		device.obexChangeDir("..");
	}

	private void printHelp(UserInterface ui) {
		ui.println("available commands:");
		ui.println("  at<command>, e.g. at^scfg? shows current device settings");
		ui.println("  cd <directory>");
		ui.println("  mkdir <directory>");
		ui.println("  dir (or ls)");
		ui.println("  put <localFilepath> <optional: deviceFilename>");
		ui.println("  get <deviceFilename> <optional: localFilepath>");
		ui.println("  cat <deviceFilename>");
		ui.println("  rm (or del) <deviceFilename>");
		ui.println("  erasedisk (Attention!! Clears all content!!)");
		ui.println("  sleep <milliseconds>");
		ui.println("  help");
		ui.println("  exit");
	}

	private void printUsage() {
		System.out.println("usage:");
		System.out.println("  tc65sh [OPTIONS]");
		System.out.println("");
		System.out.println("OPTIONS are:");
		System.out.println("");
		System.out.println("  -p --portname <portname>");
		System.out.println("        Sets the serial portname.");
		System.out.println("        Use COM1, COM2, .. on windows");
		System.out.println("        or /dev/ttyS0 or similar on linux");
		System.out.println("        Default is " + DEFAULT_PORTNAME);
		System.out.println("");
		System.out.println("  -b --baudrate <baudrate>");
		System.out.println("         Sets the baudrate. Default is " + DEFAULT_BAUDRATE);
		System.out.println("");
		System.out.println("  -fc --flowcontrol <flowcontrol>");
		System.out.println("        n for NONE");
		System.out.println("        r for RTSCTS");
		System.out.println("        x for XONXOFF");
		System.out.println("        Default is " + DEFAULT_FLOWCONTROL);
		System.out.println("        (If you're running linux, you may want to try 'n')");
		System.out.println("");
		System.out.println("  -w --wait <seconds>");
		System.out.println("        Wait X seconds for the modem to become ready");
		System.out.println("        (the modem sends ^SYSSTART if it is powered up)");
		System.out.println("        Default is 0 (means: do not wait)");
		System.out.println("");
		System.out.println("  -d --debug");
		System.out.println("        Puts out messages useful for debugging");
		System.out.println("");
		System.out.println("  -at --at-timeout");
		System.out.println("        Sets the timeout for AT commands in milliseconds");
		System.out.println("        Default is " + Device.DEFAULT_AT_COMMAND_RESPONSE_TIMEOUT + " ms");
		System.out.println("");
		System.out.println("  -ot --obex-timeout");
		System.out.println("        Sets the timeout for OBEX commands in milliseconds");
		System.out.println("        Default is " + Device.DEFAULT_OBEX_RESPONSE_TIMEOUT + " ms");
		System.out.println("");
		System.out.println("  -q --quiet");
		System.out.println("        Say (almost) nothing");
		System.out.println("");
		System.out.println("  -f --file <commandFile>");
		System.out.println("        Do not enter interactive mode but read commands");
		System.out.println("        from a command file instead");
		System.out.println("");
		System.out.println("  -c --commands <commands>");
		System.out.println("        Do not enter interactive mode but execute the given");
		System.out.println("        commands. Commands are separated by semikolon.");
		System.out.println("        Example: ... -c \"mkdir tmp;cd tmp;put test.txt\"");
		System.out.println("");
		System.out.println("  -t --telnet <telnetPort>");
		System.out.println("        Do not enter interactive mode but read commands");
		System.out.println("        from a telnet connection (a TCP/IP socket).");
		System.out.println("");
		System.out.println("  -h --help");
		System.out.println("        shows this help screen");
		System.out.println("");
	}

	private void printFolderListing(List<FileInfo> listing, UserInterface ui) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		for (FileInfo fi : listing) {
			String s = "";
			if (fi.date != null) {
				s += df.format(fi.date);
			} else {
				s += "????-??-?? ??:??:??";
			}
			if (fi.directory) {
				s += " <DIR>     ";
			} else {
				s += " " + pad10("" + fi.filesize);
			}
			s += "   " + fi.name;
			ui.println(s);
		}
	}

	private String pad10(String s) {
		while (s.length() < 10) {
			s = " " + s;
		}
		return s;
	}

	private FileHolder loadLocalFile(String filepath) throws IOException {
		File f = new File(filepath);
		FileInputStream in = new FileInputStream(f);
		byte[] buf = new byte[(int) f.length()];
		in.read(buf);
		in.close();
		ByteArray content = new ByteArray(buf);
		FileInfo fi = new FileInfo(false, f.getName(), (int) f.length(), new Date(f.lastModified()));
		FileHolder fh = new FileHolder(fi, content);
		return fh;
	}

	private void saveLocalFile(FileHolder fileHolder) throws IOException {
		File f = new File(fileHolder.fileInfo.name);
		FileOutputStream out = new FileOutputStream(f);
		out.write(fileHolder.content.getBuffer());
		out.close();
	}

}
