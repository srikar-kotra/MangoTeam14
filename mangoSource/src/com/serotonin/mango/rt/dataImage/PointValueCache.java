/*
    Mango - Open Source M2M - http://mango.serotoninsoftware.com
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc.
    @author Matthew Lohbihler
    
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.serotonin.mango.rt.dataImage;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.mango.db.dao.PointValueDao;

/**
 * This class maintains an ordered list of the most recent values for a data point. It will mirror values in the
 * database, but provide a much faster lookup for a limited number of values.
 * <p>
 * Because there is not a significant performance problem for time-based lookups, they are not handled here, but rather
 * are still handled by the database.
 *
 * @author Matthew Lohbihler
 */
public class PointValueCache {
    private PointValueCacheProduct pointValueCacheProduct = new PointValueCacheProduct();
	private final int dataPointId;
    private final int defaultSize;
    private final PointValueDao dao;

    public PointValueCache(int dataPointId, int defaultSize) {
        this.dataPointId = dataPointId;
        this.defaultSize = defaultSize;
        dao = new PointValueDao();

        if (defaultSize > 0)
            pointValueCacheProduct.refreshCache(defaultSize, this.dao, this.dataPointId);
    }

    public void savePointValue(PointValueTime pvt, SetPointSource source, boolean logValue, boolean async) {
        pointValueCacheProduct.savePointValue(pvt, source, logValue, async, this.dao, this.dataPointId);
    }

    /**
     * Saves the given value to the database without adding it to the cache.
     */
    void logPointValueAsync(PointValueTime pointValue, SetPointSource source) {
        // Save the new value and get a point value time back that has the id and annotations set, as appropriate.
        dao.savePointValueAsync(dataPointId, pointValue, source);
    }

    public PointValueTime getLatestPointValue() {
        return pointValueCacheProduct.getLatestPointValue(this.dao, this.dataPointId);
    }

    public List<PointValueTime> getLatestPointValues(int limit) {
        return pointValueCacheProduct.getLatestPointValues(limit, this.dao, this.dataPointId);
    }

    /**
     * Never manipulate the contents of this list!
     */
    public List<PointValueTime> getCacheContents() {
        return pointValueCacheProduct.getCache();
    }

    public void reset() {
        List<PointValueTime> c = pointValueCacheProduct.getCache();

        int size = defaultSize;
        if (c.size() < size)
            size = c.size();

        List<PointValueTime> nc = new ArrayList<PointValueTime>(size);
        nc.addAll(c.subList(0, size));

        pointValueCacheProduct.setMaxSize(size);
        pointValueCacheProduct.setCache(c);
    }
}
