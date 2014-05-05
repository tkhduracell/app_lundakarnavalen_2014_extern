package se.lundakarnevalen.extern.android.locationparser;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListFormatException;
import com.dd.plist.PropertyListParser;



public class ParserMain {

	public static void main( String[] args ) throws IOException, PropertyListFormatException, ParseException, ParserConfigurationException, SAXException  {
		File file = new File("Places.xml");
		NSDictionary rootDict = (NSDictionary)PropertyListParser.parse(file);
		String[] locationNames = rootDict.allKeys();
		
		NSDictionary dict1 = (NSDictionary) rootDict.get("smånöjen");
		NSArray positions = (NSArray) dict1.get("positions");
		for (NSObject position : positions.getArray()) {
			NSDictionary position2 = (NSDictionary) position;
			NSDictionary position3 = (NSDictionary) position2.get("position");
			Double latitude = (Double) position3.get("latitude").toJavaObject();
			Double longitude = (Double) position3.get("longitude").toJavaObject();
			String name = position2.get("name").toString();
			
			System.out.println(name + " @ " + longitude + " , " + latitude);
		}
		
//		for (String locationName : locationNames) {
//			NSDictionary info = (NSDictionary) rootDict.get(locationName);
//			System.out.println(locationName);
//			for (String key : info.allKeys()) {
//				//System.out.println(key);
//			}
//			
//		}
		
		
		
	}

}
