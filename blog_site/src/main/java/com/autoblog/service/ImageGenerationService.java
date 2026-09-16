package com.autoblog.service;

import com.autoblog.model.GeneratedDraft;
import com.autoblog.model.GeneratedImage;
import com.autoblog.model.WordpressSite;

public interface ImageGenerationService {
    GeneratedImage generateFeaturedImage(WordpressSite site, GeneratedDraft draft);
    GeneratedImage generateInlineImage(WordpressSite site, GeneratedDraft draft, String sectionTitle, int index);
}
