package com.aledgroup.takepic.Utils;

/**
 * Created by aled on 04/28/2016.
 */
import java.util.UUID;

public class RandomStringUUID {

    public static String GUID() {
        // Creating a random UUID (Universally unique identifier).
        UUID uuid = UUID.randomUUID();
        return  uuid.toString();
    }
}
