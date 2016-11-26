/**
 * 
 */
package com.sshtools.rfbserver.files.uvnc;

public class RFBDrive {
    public static final String UNKNOWN = "Unknown";
	public static final String NETWORK = "Network";
    public static final String CD_ROM = "CD-Rom";
    public static final String LOCAL_DISK = "Local Disk";
    public static final String FLOPPY = "Floppy";
    private String name;
	private String type;


    public RFBDrive() {
    }
    
	public RFBDrive(String name, String type) {
		this.name = name;
		this.type = type;
	}

	public RFBDrive(String spec) {
		name = spec.substring(0, 2);
		char typeCode = spec.charAt(2);
		setType(typeCode);
	}

    public void setType(char typeCode) {
        switch (typeCode) {
		case 'f':
			type = FLOPPY;
			break;
		case 'l':
			type = LOCAL_DISK;
			break;
		case 'c':
			type = CD_ROM;
			break;
		case 'n':
			type = NETWORK;
			break;
		default:
			type = UNKNOWN;
			break;
		}
    }
	
	public char toCode() {
	    if(type.equals(FLOPPY)) {
	        return 'f';
	    }
	    else if(type.equals(LOCAL_DISK)) {
            return 'l';
        }
        else if(type.equals(CD_ROM)) {
            return 'c';
        }
        else if(type.equals(NETWORK)) {
            return 'n';
        }
        else  {
            return 'u';
        }
	}

	public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	@Override
	public String toString() {
		return "RFBDrive [name=" + name + ", type=" + type + "]";
	}
}