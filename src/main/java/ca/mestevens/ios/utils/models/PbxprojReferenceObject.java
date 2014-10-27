package ca.mestevens.ios.utils.models;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class PbxprojReferenceObject {
	
	@Getter
	private String objectId;
	@Getter
	private String object;
	private int numberOfTabs;
	
	public PbxprojReferenceObject(String object) {
		this.object = object;
		String[] lines = object.split("\n");
		if (lines.length > 0) {
			String line = lines[0];
			while(line.charAt(numberOfTabs) == '\t') {
				numberOfTabs++;
			}
			objectId = object.substring(numberOfTabs, object.indexOf(' ', numberOfTabs));
		}
	}
	
	public void setObjectId(String objectId) {
		object = object.replaceFirst(this.objectId, objectId);
		this.objectId = objectId;
	}
	
	public String getProperty(String propertyName) {
		int propertyIndex = object.indexOf(propertyName);
		if (propertyIndex == -1) {
			return null;
		}
		String possiblyCommentedString =  object.substring(propertyIndex + propertyName.length() + 3, object.indexOf(";", propertyIndex));
		possiblyCommentedString = possiblyCommentedString.replaceAll("\\/\\*[^\\/]*\\*\\/$", "").trim();
		return possiblyCommentedString;
	}
	
	public void setProperty(String propertyName, String value) {
		setProperty(propertyName, value, null);
	}
	
	public void setProperty(String propertyName, String value, String comment) {
		int propertyIndex = object.indexOf(propertyName);
		String newProperty = tabString() + "\t" + propertyName + " = " + value;
		if (comment != null) {
			newProperty += " /* " + comment + " */";
		}
		newProperty += ";";
		if (propertyIndex == -1) {
			newProperty += "\n";
			int lastIndex = object.lastIndexOf(tabString() + "};");
			object = object.substring(0, lastIndex) + newProperty + tabString() + "};";
		} else {
			String oldProperty = object.substring(propertyIndex, object.indexOf(";", propertyIndex) + 1);
			object = object.replace(oldProperty, newProperty);
		}
	}
	
	public List<String> getPropertyList(String propertyName) {
		int index = object.indexOf("\n" + tabString() + "\t" + propertyName + " = (");
		String endString = "\n" + tabString() + "\t);";
		String listString = object.substring(index, object.indexOf(endString, index) + endString.length());
		String[] splitString = listString.split("\n");
		List<String> returnList = new ArrayList<String>();
		for(int i = 2; i < splitString.length - 1; i++) {
			String line = splitString[i];
			if (line.endsWith(",")) {
				line = line.substring(0, line.length() - 1);
			}
			line = line.replaceAll("\\/\\*[^\\/]*\\*\\/$", "").trim();
			returnList.add(line);
		}
		return returnList;
	}
	
	public void addToPropertyList(String propertyName, String value) {
		addToPropertyList(propertyName, value, null);
	}
	
	public void addToPropertyList(String propertyName, String value, String comment) {
		String newProperty = "\n" + tabString() + "\t\t" + value;
		if (comment != null) {
			newProperty += " /* " + comment + " */";
		}
		newProperty += ",";
		
		int index = object.indexOf("\n" + tabString() + "\t" + propertyName + " = (");
		
		if (index == -1) {
			newProperty = "\n" + tabString() + "\t" + propertyName + " = (" + newProperty + "\n" + tabString() + "\t" + ");";
			String endString = "\n" + tabString() + "};";
			object = object.replace(endString, newProperty + endString);
		} else {
			String endString = "\n" + tabString() + "\t);";
			object = object.replace(endString, newProperty + endString);
		}
	}
	
	private String tabString() {
		String returnString = "";
		for (int i = 0; i < numberOfTabs; i++) {
			returnString += "\t";
		}
		return returnString;
	}

}
