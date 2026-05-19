package com.project.artconnect.model;

import java.time.LocalDateTime;

public class Workshop {
    private long   eventId;       // ← ajouté pour appeler fn_workshop_participant_count
    private String title;
    private LocalDateTime date;
    private int durationMinutes;
    private int maxParticipants;
    private double price;
    private Artist instructor;
    private String location;
    private String description;
    private String level;

    public Workshop() {}

    public Workshop(String title, LocalDateTime date, Artist instructor, double price) {
        this.title = title;
        this.date = date;
        this.instructor = instructor;
        this.price = price;
    }

    public long   getEventId()           { return eventId; }
    public void   setEventId(long id)    { this.eventId = id; }

    public String getTitle()             { return title; }
    public void   setTitle(String t)     { this.title = t; }

    public LocalDateTime getDate()       { return date; }
    public void   setDate(LocalDateTime d){ this.date = d; }

    public int    getDurationMinutes()   { return durationMinutes; }
    public void   setDurationMinutes(int v){ this.durationMinutes = v; }

    public int    getMaxParticipants()   { return maxParticipants; }
    public void   setMaxParticipants(int v){ this.maxParticipants = v; }

    public double getPrice()             { return price; }
    public void   setPrice(double v)     { this.price = v; }

    public Artist getInstructor()        { return instructor; }
    public void   setInstructor(Artist a){ this.instructor = a; }

    public String getLocation()          { return location; }
    public void   setLocation(String v)  { this.location = v; }

    public String getDescription()       { return description; }
    public void   setDescription(String v){ this.description = v; }

    public String getLevel()             { return level; }
    public void   setLevel(String v)     { this.level = v; }

    @Override
    public String toString() { return title; }
}