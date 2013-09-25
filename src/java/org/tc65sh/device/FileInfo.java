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

import java.text.SimpleDateFormat;
import java.util.Date;

public class FileInfo implements Comparable<FileInfo> {

	private static SimpleDateFormat DTF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public boolean directory;
	public String name = null;
	public int filesize = -1;
	public Date date = null;

	public FileInfo(String name) {
		this(false,name,-1,null);
	}
	
	public FileInfo(boolean directory, String name, int filesize, Date date) {
		super();
		this.directory = directory;
		this.name = name;
		this.filesize = filesize;
		this.date = date;
	}

	@Override
	public int compareTo(FileInfo o) {
		int result = 0;
		if ( directory != o.directory ) {
			result = directory ? -1 : 1;
		} else if ( name != null && o.name != null ){
			return name.compareTo(o.name);
		}
		return result;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if ( date != null ) {
			sb.append(DTF.format(date));
		} else {
			sb.append("                   ");
		}
		sb.append("  ");
		if ( directory ) {
			sb.append("<DIR>     ");
		} else if ( filesize == -1 ) {
			sb.append("          ");
		} else {
			String ssize = ""+filesize;
			for( int i=0 ; i<10-ssize.length() ; i++ ) {
				sb.append(' ');
			}
			sb.append(ssize);
		}
		sb.append("  ");
		sb.append(name);
		return sb.toString();
	}

	
}
