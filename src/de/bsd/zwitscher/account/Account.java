package de.bsd.zwitscher.account;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * One server account
 *
 * @author Heiko W. Rupp
 */
public class Account implements Parcelable {
    private int id;
    private String name;
    private String accessTokenKey;
    private String accessTokenSecret;
    private String serverType;
    private String serverUrl;
    private boolean defaultAccount;
    private String password;

    public Account(int id, String name, String accessTokenKey, String accessTokenSecret, String serverUrl, String serverType,boolean defaultAccount) {
        this.id = id;
        this.name = name;
        this.accessTokenKey = accessTokenKey;
        this.accessTokenSecret = accessTokenSecret;
        this.serverType = serverType;
        this.serverUrl = serverUrl;
        this.defaultAccount = defaultAccount;
    }

    public Account(int id, String name, String serverUrl, String serverType, boolean defaultAccount, String password) {
        this.id = id;
        this.name = name;
        this.serverUrl = serverUrl;
        this.serverType = serverType;
        this.defaultAccount = defaultAccount;
        this.password = password;
    }

    public Account(Parcel parcel) {
        id = parcel.readInt();
        name = parcel.readString();
        accessTokenKey = parcel.readString();
        accessTokenSecret = parcel.readString();
        serverType = parcel.readString();
        serverUrl  = parcel.readString();
        defaultAccount = parcel.readInt() == 1;
        password = parcel.readString();

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isStatusNet() {
        return !serverType.equalsIgnoreCase("twitter");

    }
    /**
     * Return a canonical representation of this account that can be used
     * within various places in the UI
     * @return Human readable identifier of this account
     */
    public String getAccountIdentifier() {
        if (serverUrl!=null && !(serverUrl.length() == 0)) {
            return name + "@" + serverUrl;
        }
        else {
            return name + "@" + serverType;
        }
    }

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
        parcel.writeString(password);
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

    /**
     * Return account details. For a human readable representation
     * see {@link #getAccountIdentifier}
     * @return String describing the account.
     */
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Account");
        sb.append("{id=").append(id);
        sb.append(", name='").append(name).append('\'');
        sb.append(", serverType='").append(serverType).append('\'');
        sb.append(", serverUrl='").append(serverUrl).append('\'');
        sb.append(", defaultAccount=").append(defaultAccount);
        sb.append(", accessToken='").append(accessTokenSecret != null ? "-set-" : "-unset-").append('\'');
        sb.append(", password='").append(password!=null?"-set-":"-unset-").append('\'');
        sb.append('}');
        return sb.toString();
    }

    public String getStatusType() {
        if (isStatusNet())
            return "Dent";
        else
            return "Tweet";
    }
}
