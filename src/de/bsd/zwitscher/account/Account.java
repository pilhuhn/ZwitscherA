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

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Account account = (Account) o;

        return id == account.id;

    }

    public int hashCode() {
        return id;
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Account");
        sb.append("{id=").append(id);
        sb.append(", name='").append(name).append('\'');
        sb.append(", serverType='").append(serverType).append('\'');
        sb.append(", serverUrl='").append(serverUrl).append('\'');
        sb.append(", defaultAccount=").append(defaultAccount);
        sb.append(", accessTokenSecret='").append(accessTokenSecret!=null?"-set-":"-unset-").append('\'');
        sb.append('}');
        return sb.toString();
    }
}
