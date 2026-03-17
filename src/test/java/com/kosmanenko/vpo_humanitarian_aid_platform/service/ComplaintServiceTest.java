package com.kosmanenko.vpo_humanitarian_aid_platform.service;

import com.kosmanenko.vpo_humanitarian_aid_platform.model.Announcement;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.Complaint;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.ComplaintRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @InjectMocks
    private ComplaintService complaintService;

    private final Announcement announcement = Announcement.builder().id(1L).title("Тест").build();
    private final User complainant = User.builder().id(2L).email("user@test.com").build();

    @Test
    @DisplayName("submit — зберігає нову скаргу зі статусом PENDING")
    void submit_savesComplaint_withPendingStatus() {
        when(complaintRepository.existsByAnnouncementAndComplainant(announcement, complainant))
            .thenReturn(false);
        ArgumentCaptor<Complaint> captor = ArgumentCaptor.forClass(Complaint.class);
        when(complaintRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        complaintService.submit(announcement, complainant, "Шахрайство");

        assertThat(captor.getValue().getReason()).isEqualTo("Шахрайство");
        assertThat(captor.getValue().getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("submit — кидає виняток при повторній скарзі")
    void submit_throwsException_whenDuplicateComplaint() {
        when(complaintRepository.existsByAnnouncementAndComplainant(announcement, complainant))
            .thenReturn(true);

        assertThatThrownBy(() -> complaintService.submit(announcement, complainant, "Причина"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("вже подавали скаргу");
    }

    @Test
    @DisplayName("findById — повертає скаргу якщо існує")
    void findById_returnsComplaint_whenExists() {
        Complaint complaint = Complaint.builder().id(1L).build();
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(complaint));

        assertThat(complaintService.findById(1L)).isPresent();
    }

    @Test
    @DisplayName("markReviewed — встановлює статус REVIEWED")
    void markReviewed_setsReviewedStatus() {
        Complaint complaint = Complaint.builder().id(1L).status("PENDING").build();
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        complaintService.markReviewed(1L);

        assertThat(complaint.getStatus()).isEqualTo("REVIEWED");
    }
}
