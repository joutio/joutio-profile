package com.joutio.profile.application.handler.query;

import com.joutio.profile.domain.model.ImageReferenceCount;
import com.joutio.profile.domain.port.out.ProfileRepositoryPort;
import com.joutio.profile.domain.query.ProfileQuery;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileQueryHandlerTest {

    private final ProfileRepositoryPort profileRepository = mock(ProfileRepositoryPort.class);
    private final ProfileQueryHandler handler = new ProfileQueryHandler(profileRepository);

    @Test
    void image_references_delegates_to_repository() {
        List<String> urls = List.of("https://media.joutio.com/joutio-media/uploads/a.jpg");
        List<ImageReferenceCount> counts = List.of(new ImageReferenceCount(urls.getFirst(), 2));
        when(profileRepository.countReferencesByBannerUrls(urls)).thenReturn(counts);

        assertThat(handler.handle(new ProfileQuery.CountImageReferencesQuery(urls)))
                .containsExactlyElementsOf(counts);
        verify(profileRepository).countReferencesByBannerUrls(urls);
    }
}
