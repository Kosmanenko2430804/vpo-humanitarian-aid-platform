package com.kosmanenko.vpo_humanitarian_aid_platform.event;

import com.kosmanenko.vpo_humanitarian_aid_platform.model.Announcement;

public record AnnouncementSubmittedEvent(Announcement announcement, String message) {}
