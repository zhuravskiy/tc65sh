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

import java.io.UnsupportedEncodingException;

public class ByteArray {

	public static void main(String[] args) {
		ByteArray b = new ByteArray();
		b.append((byte)'A');
		System.out.println("A = "+b.toPrintableString());
		b = new ByteArray(new byte[] {'A','B','C'});
		System.out.println("ABC = "+b.toPrintableString());
		b = b.subArray(1, 1);
		System.out.println("B = "+b.toPrintableString());
		b = new ByteArray(new byte[]{(byte)0x80, 0x13});
		System.out.println("80 13 = "+b.toHexString());
	}
	
	
	private byte[] buffer;

	public ByteArray() {
		buffer = new byte[0];
	}

	public ByteArray(String input, String charsetName) {
		try {
			buffer = input.getBytes(charsetName); //"ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public ByteArray(byte[] input, int offset, int length) {
		buffer = new byte[0];
		append(input, offset, length);
	}

	public ByteArray(byte[] input) {
		buffer = new byte[0];
		append(input,0,input.length);
	}

	public byte[] toNewByteArray() {
		byte[] result = new byte[buffer.length];
		System.arraycopy(buffer, 0, result, 0, buffer.length);
		return result;
	}

	public byte[] getBuffer() {
		return buffer;
	}

	public int length() {
		return buffer.length;
	}

	public void append(ByteArray input) {
		append(input.getBuffer());
	}

	public void append(byte[] input, int offset, int length) {
		byte[] b = new byte[buffer.length + length];
		System.arraycopy(buffer, 0, b, 0, buffer.length);
		System.arraycopy(input, offset, b, buffer.length, length);
		buffer = b;
	}

	public void append(byte[] input) {
		append(input,0,input.length);
	}

	public void append(byte input) {
		byte[] b = new byte[buffer.length + 1];
		System.arraycopy(buffer, 0, b, 0, buffer.length);
		b[b.length-1] = input;
		buffer = b;
	}

	public void append(String input, String charsetName) {
		byte[] b;
		try {
			b = input.getBytes(charsetName);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		append(b);
	}


	public void append(int input) {
		append((byte)input);
	}

	public String toPrintableString() {
		StringBuilder sb = new StringBuilder(buffer.length);
		for (byte b : buffer) {
			if (b < 0x20 || b > 0x7F) {
				sb.append('.');
			} else {
				sb.append((char) b);
			}
		}
		return sb.toString();
	}

	public String toHexString() {
		StringBuilder sb = new StringBuilder(buffer.length * 4);
		for (byte b : buffer) {
			sb.append('0');
			sb.append('x');
			int i = b;
			if ( i < 0 ) {
				i += 0x100;
			}
			if ( i < 16 ) {
				sb.append('0');
			}
			sb.append( Integer.toHexString(i) );
			sb.append(' ');
		}
		return sb.toString();
	}

	public ByteArray subArray(int offset, int length) {
		ByteArray r = new ByteArray(buffer, offset, length);
		return r;
	}

}
