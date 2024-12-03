package io.github.btmxh.apartmentapp;

import javafx.beans.property.*;
import java.time.LocalDateTime;

public class Payment {
    public static final int NULL_ID = -1;
    private int id = -1;
    private final ObjectProperty<ServiceFee> fee;
    private final StringProperty roomId;
    private final LongProperty amount;
    private final ObjectProperty<LocalDateTime> committedTimestamp;
    private final StringProperty roomOwner; // Normal StringProperty for roomOwner

    public Payment(int id, ServiceFee fee, String roomId, long amount, LocalDateTime committedTimestamp, String roomOwner) {
        this.id = id;
        this.fee = new SimpleObjectProperty<>(fee);
        this.roomId = new SimpleStringProperty(roomId);
        this.amount = new SimpleLongProperty(amount);
        this.committedTimestamp = new SimpleObjectProperty<>(committedTimestamp);
        this.roomOwner = new SimpleStringProperty(roomOwner); // Initialize roomOwner
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    // Getters and Setters for feeId
    public ServiceFee getFee() {
        return fee.get();
    }

    public void setFee(ServiceFee fee) {
        this.fee.set(fee);
    }

    // Getters and Setters for roomId
    public String getRoomId() {
        return roomId.get();
    }

    public void setRoomId(String roomId) {
        this.roomId.set(roomId);
    }

    public StringProperty roomIdProperty() {
        return roomId;
    }

    // Getters and Setters for amount
    public long getAmount() {
        return amount.get();
    }

    public void setAmount(long amount) {
        this.amount.set(amount);
    }

    public LongProperty amountProperty() {
        return amount;
    }

    // Getters and Setters for committedTimestamp
    public LocalDateTime getCommittedTimestamp() {
        return committedTimestamp.get();
    }

    public void setCommittedTimestamp(LocalDateTime committedTimestamp) {
        this.committedTimestamp.set(committedTimestamp);
    }

    public ObjectProperty<LocalDateTime> committedTimestampProperty() {
        return committedTimestamp;
    }

    // Getters and Setters for roomOwner
    public String getRoomOwner() {
        return roomOwner.get();
    }

    public void setRoomOwner(String roomOwner) {
        this.roomOwner.set(roomOwner);
    }

    public StringProperty roomOwnerProperty() {
        return roomOwner;
    }
}
