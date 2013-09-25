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

import java.util.ArrayList;

/**
 * bla blubb fasel du    => {bla, blubb, fasel}
 * bla "blubb fasel" du  => {bla, blubb fasel, du}
 */
public abstract class StringSplitter {

	public static void main(String[] args) {
		System.out.println("a = "+split("a")[0]);
		System.out.println("a = "+split("a b c d")[0]);
		System.out.println("c = "+split("a b c d")[2]);
		System.out.println("b c = "+split("a \"b c\" d")[1]);
		System.out.println("a = "+split("a \"b c\"")[0]);
		System.out.println("b c = "+split("a \"b c")[1]);
	}
	
	
	public static String[] split(String input) {
		if ( ! input.contains("\"") ) return input.split(" ");
		ArrayList<String> l = new ArrayList<String>();
		boolean isInQuotes = false;
		String stack = "";
		for( int i=0 ; i<input.length() ; i++ ) {
			if ( input.charAt(i) == '\"' ) {
				if( isInQuotes ) {
					if ( stack.length() > 0) {
						l.add(stack);
						stack = "";
					}
					isInQuotes = false;
				} else {
					isInQuotes = true;
				}
			} else if ( input.charAt(i) == ' ' ) {
				if ( isInQuotes ) {
					stack += " ";
				} else {
					if ( stack.length() > 0) {
						l.add(stack);
						stack = "";
					}
				}
			} else {
				stack += input.charAt(i);
			}
		}
		if ( stack.length() > 0) {
			l.add(stack);
			stack = "";
		}
		return l.toArray(new String[l.size()]);
	}
}
