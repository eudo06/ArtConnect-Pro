package com.project.artconnect.service.impl;

import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.model.Review;
import com.project.artconnect.persistence.JdbcCommunityMemberDao;
import com.project.artconnect.service.CommunityService;

import java.util.List;
import java.util.Optional;

public class JdbcCommunityService implements CommunityService {
    private final JdbcCommunityMemberDao communityMemberDao;

    public JdbcCommunityService() {
        this(new JdbcCommunityMemberDao());
    }

    public JdbcCommunityService(JdbcCommunityMemberDao communityMemberDao) {
        this.communityMemberDao = communityMemberDao;
    }

    @Override
    public List<CommunityMember> getAllMembers() {
        return communityMemberDao.findAll();
    }

    @Override
    public Optional<CommunityMember> getMemberByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return communityMemberDao.findByName(name);
    }

    @Override
    public List<Review> getReviewsByMember(CommunityMember member) {
        if (member == null || member.getName() == null || member.getName().isBlank()) {
            return List.of();
        }
        return communityMemberDao.findReviewsByMemberName(member.getName());
    }

    public List<Review> getReviewsByArtworkTitle(String artworkTitle) {
        if (artworkTitle == null || artworkTitle.isBlank()) return List.of();
        return communityMemberDao.findReviewsByArtworkTitle(artworkTitle);
    }

    public void addReview(String artworkTitle, String memberName, int rating, String comment) {
        communityMemberDao.createReview(artworkTitle, memberName, rating, comment);
    }
}
