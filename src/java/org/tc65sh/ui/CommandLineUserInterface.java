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
package org.tc65sh.ui;

import java.util.LinkedList;
import java.util.StringTokenizer;

public class CommandLineUserInterface implements UserInterface {

	private LinkedList<String> commands = new LinkedList<String>();
	
	public CommandLineUserInterface( String commandLineCommands ) {
		StringTokenizer tok = new StringTokenizer(commandLineCommands,";");
		
		while( tok.hasMoreTokens() ) {
			String cmd = tok.nextToken().trim();
			if ( cmd.length() > 0 ) {
				commands.add(cmd);
			}
		}
	}
	
	@Override
	public String readCommand() {
		if ( commands.isEmpty() ) return null;
		return commands.removeFirst();
	}
	
	@Override
	public void println(String message) {
		println(message, null);
	}

	@Override
	public void println(String message, Throwable t) {
		System.out.println(message);
		if ( t != null ) {
			t.printStackTrace(System.out);
		}
	}

	@Override
	public void echoCommand(String cmdline) {
		System.out.println(cmdline);
	}

}
