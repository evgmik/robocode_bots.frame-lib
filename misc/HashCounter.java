// -*- java -*-
package eem.frame.misc;

import java.util.HashMap;
import java.util.Set;

public class HashCounter<T> {
	public HashMap<T,Integer> map = new HashMap<T, Integer>();

	//helper method to increment count in HashMap
	public void incrHashCounter( T key ) {
		Integer cnt = getHashCounter( key );
		cnt++;
		map.put( key, cnt );
	}

	public Integer getHashCounter( T key ) {
		if ( map.containsKey( key ) ) {
			return map.get( key );
		} else {
			return 0;
		}
	}

	public Set<T> keySet() {
		return map.keySet();
	}
}
