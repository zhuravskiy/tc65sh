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


public class FileHolder {

	public FileInfo fileInfo;
	public ByteArray content;

	public FileHolder(FileInfo fileInfo) {
		this(fileInfo,null);
	}

	public FileHolder(FileInfo fileInfo, ByteArray content) {
		super();
		this.fileInfo = fileInfo;
		this.content = content;
	}

}
