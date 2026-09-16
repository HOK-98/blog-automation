package com.autoblog.service;

import com.autoblog.model.GenerationPreview;

public interface GenerationService {
    GenerationPreview generatePreview(String keyword);
}
