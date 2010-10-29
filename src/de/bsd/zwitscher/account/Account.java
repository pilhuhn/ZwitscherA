/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package de.bsd.zwitscher.account;


/**
 * One server account
 *
 * @author Heiko W. Rupp
 */
public class Account {
    String name;
    String accessTokenKey;
    String accessTokenSecret;
    String serverType;
    String serverUrl;

    public Account(String name, String accessTokenKey, String accessTokenSecret, String serverType, String serverUrl) {
        this.name = name;
        this.accessTokenKey = accessTokenKey;
        this.accessTokenSecret = accessTokenSecret;
        this.serverType = serverType;
        this.serverUrl = serverUrl;
    }

    public Account(String name, String serverType) {
        this.name = name;
        this.serverType = serverType;
    }

    public String getName() {
        return name;
    }

    public String getAccessTokenKey() {
        return accessTokenKey;
    }

    public void setAccessTokenKey(String accessTokenKey) {
        this.accessTokenKey = accessTokenKey;
    }

    public String getAccessTokenSecret() {
        return accessTokenSecret;
    }

    public void setAccessTokenSecret(String accessTokenSecret) {
        this.accessTokenSecret = accessTokenSecret;
    }

    public String getServerType() {
        return serverType;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }
}
