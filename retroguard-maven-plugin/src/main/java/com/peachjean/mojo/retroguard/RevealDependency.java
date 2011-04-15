package com.peachjean.mojo.retroguard;

import org.apache.commons.lang.StringUtils;

/**
 * Created by IntelliJ IDEA.
 * User: jbunting
 * Date: 4/8/11
 * Time: 10:36 AM
 * To change this template use File | Settings | File Templates.
 */
public class RevealDependency {
    private String groupId;
    private String artifactId;
    private String obfuscatedClassifier;
    private String unobfuscatedClassifier;
    private String obfuscatedType;
    private String unobfuscatedType;

    public String getObfuscatedIdString() {
        if(obfuscatedClassifier == null)
        {
            return StringUtils.join(new String[] { groupId, artifactId, obfuscatedType }, ":");
        }
        else
        {
            return StringUtils.join(new String[] { groupId, artifactId, obfuscatedType, obfuscatedClassifier }, ":");
        }
    }

    public String getUnobfuscatedIdString() {
        if(unobfuscatedClassifier == null)
        {
            return StringUtils.join(new String[] { groupId, artifactId, unobfuscatedType }, ":");
        }
        else
        {
            return StringUtils.join(new String[] { groupId, artifactId, unobfuscatedType, unobfuscatedClassifier }, ":");
        }
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getObfuscatedClassifier() {
        return obfuscatedClassifier;
    }

    public void setObfuscatedClassifier(String obfuscatedClassifier) {
        this.obfuscatedClassifier = obfuscatedClassifier;
    }

    public String getUnobfuscatedClassifier() {
        return unobfuscatedClassifier;
    }

    public void setUnobfuscatedClassifier(String unobfuscatedClassifier) {
        this.unobfuscatedClassifier = unobfuscatedClassifier;
    }

    public String getObfuscatedType() {
        return obfuscatedType;
    }

    public void setObfuscatedType(String obfuscatedType) {
        this.obfuscatedType = obfuscatedType;
    }

    public String getUnobfuscatedType() {
        return unobfuscatedType;
    }

    public void setUnobfuscatedType(String unobfuscatedType) {
        this.unobfuscatedType = unobfuscatedType;
    }
}
