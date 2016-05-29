package com.munisystem.porst;

public class Student {
    private byte[] id;
    private byte[] name;

    public Student(byte[] id, byte[] name) {
        this.id = id;
        this.name = name;
    }

    public byte[] getId() {
        return id;
    }

    public void setId(byte[] id) {
        this.id = id;
    }

    public byte[] getName() {
        return name;
    }

    public void setName(byte[] name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Student{" + "\n" +
            "id   = " + NfcReader.toHex(id) + "\n" +
            "name = " + NfcReader.toHex(name) + "\n" +
            '}';
    }
}
