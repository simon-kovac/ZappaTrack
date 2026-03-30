package com.example.zappatrack;

/**
 * Model classes for leaderboard entries
 */
public class LeaderboardEntry {

    /**
     * Entry for most transferred lighters leaderboard
     */
    public static class TransferredLighter {
        private long lighterId;
        private String lighterName;
        private int transferCount;
        private String currentOwnerName;
        private long currentOwnerId;
        private String imageUrl;

        // Constructor
        public TransferredLighter(long lighterId, String lighterName, int transferCount,
                                  String currentOwnerName, long currentOwnerId, String imageUrl) {
            this.lighterId = lighterId;
            this.lighterName = lighterName;
            this.transferCount = transferCount;
            this.currentOwnerName = currentOwnerName;
            this.currentOwnerId = currentOwnerId;
            this.imageUrl = imageUrl;
        }

        // Getters
        public long getLighterId() { return lighterId; }
        public String getLighterName() { return lighterName; }
        public int getTransferCount() { return transferCount; }
        public String getCurrentOwnerName() { return currentOwnerName; }
        public long getCurrentOwnerId() { return currentOwnerId; }

        public String getImageUrl() { return imageUrl; }

        // Setters
        public void setLighterId(long lighterId) { this.lighterId = lighterId; }
        public void setLighterName(String lighterName) { this.lighterName = lighterName; }
        public void setTransferCount(int transferCount) { this.transferCount = transferCount; }
        public void setCurrentOwnerName(String currentOwnerName) { this.currentOwnerName = currentOwnerName; }
        public void setCurrentOwnerId(long currentOwnerId) { this.currentOwnerId = currentOwnerId; }
    }

    /**
     * Entry for top users (most lighters) leaderboard
     */
    public static class TopUser {
        private long userId;
        private String userName;
        private int lighterCount;
        private String profilePhotoUrl;

        // Constructor
        public TopUser(long userId, String userName, int lighterCount, String profilePhotoUrl) {
            this.userId = userId;
            this.userName = userName;
            this.lighterCount = lighterCount;
            this.profilePhotoUrl = profilePhotoUrl;
        }

        // Getters
        public long getUserId() { return userId; }
        public String getUserName() { return userName; }
        public int getLighterCount() { return lighterCount; }
        public String getProfilePhotoUrl() { return profilePhotoUrl; }

        // Setters
        public void setUserId(long userId) { this.userId = userId; }
        public void setUserName(String userName) { this.userName = userName; }
        public void setLighterCount(int lighterCount) { this.lighterCount = lighterCount; }
        public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }
    }
}