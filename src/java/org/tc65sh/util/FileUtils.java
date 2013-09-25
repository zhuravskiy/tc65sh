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
package org.tc65sh.util;

public abstract class FileUtils {

	public static String extractFilename(String path) {
		if ( path == null ) return null;
		String norm = path.replace('\\', '/');
		int i = norm.lastIndexOf('/');
		if ( i == -1 ) {
			return path;
		} else {
			return norm.substring(i+1);
		}
	}

	public static boolean isFilePath(String path) {
		if ( path == null ) return false;
		return path.contains("\\") || path.contains("/");
	}
	
	public static boolean isFileName(String path) {
		if ( path == null ) return false;
		return ! isFilePath(path);
	}
	
}
