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

import android.os.Parcel;
import android.os.Parcelable;

/**
 * One server account
 *
 * @author Heiko W. Rupp
 */
public class Account implements Parcelable {
    int id;
    String name;
    String accessTokenKey;
    String accessTokenSecret;
    String serverType;
    String serverUrl;
    boolean defaultAccount;

    public Account(int id, String name, String accessTokenKey, String accessTokenSecret, String serverUrl, String serverType,boolean defaultAccount) {
        this.id = id;
        this.name = name;
        this.accessTokenKey = accessTokenKey;
        this.accessTokenSecret = accessTokenSecret;
        this.serverType = serverType;
        this.serverUrl = serverUrl;
        this.defaultAccount = defaultAccount;
    }

    public Account(Parcel parcel) {
        id = parcel.readInt();
        name = parcel.readString();
        accessTokenKey = parcel.readString();
        accessTokenSecret = parcel.readString();
        serverType = parcel.readString();
        serverUrl  = parcel.readString();
        defaultAccount = parcel.readInt() == 1;

    }

    public int getId() {
        return id;
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

    public boolean isDefaultAccount() { return defaultAccount; }

    public int describeContents() {
        return 0;  // TODO: Customise this generated block
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeString(name);
        parcel.writeString(accessTokenKey);
        parcel.writeString(accessTokenSecret);
        parcel.writeString(serverType);
        parcel.writeString(serverUrl);
        parcel.writeInt(defaultAccount ? 1 : 0);
    }

    public static Creator<Account> CREATOR = new Creator<Account>() {
        public Account createFromParcel(Parcel parcel) {
            return new Account(parcel);
        }

        public Account[] newArray(int size) {
            return new Account[size];
        }
    };
}
