package net.cactii.flash2;

import java.io.DataOutputStream;
import java.io.IOException;

public class Su {
/*
 * This class handles 'su' functionality. Ensures that the command can be run, then
 * handles run/pipe/return flow.
 */
	public boolean can_su;
	public String su_bin_file;
	
	public Su() {
		this.can_su = true;
		this.su_bin_file = "/system/xbin/su";
		if (this.Run("echo"))
			return;
		this.su_bin_file = "/system/bin/su";
		if (this.Run("echo"))
			return;
		this.su_bin_file = "";
		this.can_su = false;	
	}
    public boolean Run(String command) {
    	DataOutputStream os = null;
    	try {
			Process process = Runtime.getRuntime().exec(this.su_bin_file);
			os = new DataOutputStream(process.getOutputStream());
			os.writeBytes(command + "\n");
			os.flush();
			os.writeBytes("exit\n");
			os.flush();
			process.waitFor();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    	return false;
    }	
}
