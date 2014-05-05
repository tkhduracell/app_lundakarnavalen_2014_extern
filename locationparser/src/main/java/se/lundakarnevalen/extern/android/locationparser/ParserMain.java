package se.lundakarnevalen.extern.android.locationparser;

import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListFormatException;
import com.dd.plist.PropertyListParser;

public class ParserMain {

	public static void main( String[] args ) throws IOException, PropertyListFormatException, ParseException, ParserConfigurationException, SAXException  {
		List<String> listAddings = new ArrayList<String>();
		List<String> strings = new ArrayList<String>();
		
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
			String nameLower = name.toLowerCase();
			nameLower = Normalizer.normalize(nameLower,
			        Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
			listAddings.add("data.add(new Dataelement(r.string.smanojen, R.string." + nameLower + ", " + latitude + "f, " + longitude + "f, " + "R.drawable.bubble_smanojen, DataType.SMALL_FUN));");
			strings.add("<string name=\"" + nameLower + "\">"+name+"</string>");
		}
		
		for (String s : listAddings) {
			System.out.println(s);

		}
		System.out.println();
		for (String s : strings) {
			System.out.println(s);

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
