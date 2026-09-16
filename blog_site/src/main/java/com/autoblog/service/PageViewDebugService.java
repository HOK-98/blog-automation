package com.autoblog.service;

import com.autoblog.model.PageViewMatchRow;
import java.util.List;

public interface PageViewDebugService {
    List<PageViewMatchRow> getMatches();
}
