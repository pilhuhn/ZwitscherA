package de.bsd.zwitscher;

/**
 * Class that holds the twitter consumer token
 */
public class Tokens {

	public static String consumerKey = "-replace me-";
	public static String consumerSecret = "-replace -me";

    // true for identi.ca ; disable if your twitter oauth token is not enabled for xAuth
    // future version of the code will have a changed logic.
    public static boolean xAuthEnabled = true;

    // TODO if you build Zwitscher on your own, you need to fill in a read it later token here
    public static final String readItLaterToken = "";

    public static String tweetMarkerToken = "";
}
