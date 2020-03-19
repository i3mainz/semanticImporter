package de.hsmainz.cs.semgis.importer.geoimporter.user;

public enum UserType {
	Administrator("administrator"),Configurer("configurer"),User("user");
	
	private final String userTypeString;
	
	private UserType(String userTypeString){
		this.userTypeString=userTypeString;
	}

	public String getUserTypeString() {
		return userTypeString;
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return userTypeString;
	}
	
	
	

}
