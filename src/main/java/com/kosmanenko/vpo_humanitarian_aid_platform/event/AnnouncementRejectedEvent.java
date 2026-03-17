package com.kosmanenko.vpo_humanitarian_aid_platform.event;

import com.kosmanenko.vpo_humanitarian_aid_platform.model.Announcement;

public record AnnouncementRejectedEvent(Announcement announcement, String reason) {}
