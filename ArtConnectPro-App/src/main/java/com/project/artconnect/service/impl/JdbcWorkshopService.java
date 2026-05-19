package com.project.artconnect.service.impl;

import com.project.artconnect.model.Booking;
import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.persistence.JdbcCommunityMemberDao;
import com.project.artconnect.persistence.JdbcWorkshopDao;
import com.project.artconnect.service.WorkshopService;

import java.util.List;
import java.util.Optional;

public class JdbcWorkshopService implements WorkshopService {
    private final JdbcWorkshopDao workshopDao;
    private final JdbcCommunityMemberDao communityMemberDao;

    public JdbcWorkshopService() {
        this(new JdbcWorkshopDao(), new JdbcCommunityMemberDao());
    }

    public JdbcWorkshopService(JdbcWorkshopDao workshopDao, JdbcCommunityMemberDao communityMemberDao) {
        this.workshopDao = workshopDao;
        this.communityMemberDao = communityMemberDao;
    }

    @Override
    public List<Workshop> getAllWorkshops() {
        return workshopDao.findAll();
    }

    @Override
    public Optional<Workshop> getWorkshopByTitle(String title) {
        if (title == null) {
            return Optional.empty();
        }
        return workshopDao.findByTitle(title);
    }

    @Override
    public void bookWorkshop(Workshop workshop, CommunityMember member) {
        if (workshop == null || member == null || workshop.getTitle() == null || member.getName() == null) {
            return;
        }
        communityMemberDao.createBooking(workshop.getTitle(), member.getName());
    }

    @Override
    public List<Booking> getBookingsByMember(CommunityMember member) {
        if (member == null || member.getName() == null || member.getName().isBlank()) {
            return List.of();
        }
        return communityMemberDao.findBookingsByMemberName(member.getName());
    }
}
