package com.digitalreasoning.mojo.retroguard;

import java.io.File;

public class Utils {

    public static final String SPEC_TYPE = "retroguard-spec";
	public static final String UNOBFUSCATED_CLASSIFIER = "unobfuscated";
    public static final String SPEC_EXTENSION = "rgs";

    public static File getArtifactFile( File basedir, String finalName, String classifier, String extension )
    {
        if ( classifier == null )
        {
            classifier = "";
        }
        else if ( classifier.trim().length() > 0 && !classifier.startsWith( "-" ) )
        {
            classifier = "-" + classifier;
        }

        return new File( basedir, finalName + classifier + "." + extension );
    }

}
