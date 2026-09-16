package com.autoblog.service;

import com.autoblog.model.GeneratedDraft;
import com.autoblog.model.GenerationPreview;
import java.util.List;

public interface GeneratedDraftService {
    GeneratedDraft savePreview(GenerationPreview preview);
    List<GeneratedDraft> getDrafts();
    List<GeneratedDraft> getDrafts(Long siteId, boolean allSites);
    GeneratedDraft getDraft(long id);
    void updateDraft(GeneratedDraft draft);
    void deleteDraft(long id);
    void deleteDrafts(long[] ids);
    void publishDraft(long id);
}
