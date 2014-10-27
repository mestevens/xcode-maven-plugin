package ca.mestevens.ios.utils.models;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class PbxprojSection {
	
	@Getter
	private String section;
	private int numberOfTabs;
	
	public PbxprojSection(String section) {
		this.section = section;
		String[] lines = section.split("\n");
		if (lines.length > 0) {
			String line = lines[0];
			while(line.charAt(numberOfTabs) == '\t') {
				numberOfTabs++;
			}
		}
	}
	
	public PbxprojReferenceObject getReferenceObject(String objectId) {
		return new PbxprojReferenceObject(getReferenceObjectString(objectId));
	}
	
	public boolean containsObjectId(String objectId) {
		String startString = "";
		startString += tabString();
		startString += objectId;
		if (section.startsWith(startString) || section.contains("\n" + startString)) {
			return true;
		}
		return false;
	}
	
	public void addReferenceObject(PbxprojReferenceObject object) {
		if (!containsObjectId(object.getObjectId())) {
			section = section.concat(object.getObject());
		}
	}
	
	public void updateReferenceObject(String objectId, PbxprojReferenceObject object) {
		String oldObject = getReferenceObjectString(objectId);
		section = section.replace(oldObject, object.getObject());
	}
	
	private String getReferenceObjectString(String objectId) {
		int objectIdIndex = section.indexOf(objectId);
		if (section.startsWith(tabString() + objectId)) {
			objectIdIndex = tabString().length();
		} else {
			objectIdIndex = section.indexOf("\n" + tabString() + objectId) + tabString().length() + 1;
		}
		String endString = "\n";
		endString += tabString();
		endString += "};";
		//Remove number of tabs for formatting
		String object = section.substring(objectIdIndex - numberOfTabs, section.indexOf(endString, objectIdIndex) + endString.length());
		return object;
	}
	
	public List<String> getObjectIds() {
		List<String> returnList = new ArrayList<String>();
		String[] lines = section.split("\n");
		for(String line : lines) {
			if (line.startsWith(tabString()) && !line.startsWith(tabString() + "\t") && !line.startsWith(tabString() + "};")) {
				line = line.trim();
				line = line.substring(0, line.indexOf(' '));
				returnList.add(line);
			}
		}
		return returnList;
	}
	
	private String tabString() {
		String returnString = "";
		for (int i = 0; i < numberOfTabs; i++) {
			returnString += "\t";
		}
		return returnString;
	}

}
