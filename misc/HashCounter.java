// -*- java -*-
package eem.frame.misc;

import java.util.HashMap;

public class HashCounter {
	public HashMap<String,Integer> map = new HashMap<String, Integer>();

	//helper method to increment count in HashMap
	public void incrHashCounter( String key ) {
		Integer cnt = getHashCounter( key );
		cnt++;
		map.put( key, cnt );
	}

	public Integer getHashCounter( String key ) {
		if ( map.containsKey( key ) ) {
			return map.get( key );
		} else {
			return 0;
		}
	}
	
}
