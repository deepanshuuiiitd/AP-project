package edu.univ.erp.util;

import org.mindrot.jbcrypt.BCrypt;

public class HashGen {
    public static void main(String[] args) {
        String[] passwords = {"admin@123", "inst@123", "stu1@123", "stu2@123"};
        for (String pw : passwords) {
            String hash = BCrypt.hashpw(pw, BCrypt.gensalt(12));
            System.out.println(pw + " -> " + hash);
        }
    }
}
