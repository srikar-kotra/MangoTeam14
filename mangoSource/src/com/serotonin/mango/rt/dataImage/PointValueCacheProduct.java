package com.serotonin.mango.rt.dataImage;


import java.util.List;
import java.util.ArrayList;
import com.serotonin.mango.db.dao.PointValueDao;

public class PointValueCacheProduct {
	private List<PointValueTime> cache = new ArrayList<PointValueTime>();
	private int maxSize = 0;

	public List<PointValueTime> getCache() {
		return cache;
	}

	public void setCache(List<PointValueTime> cache) {
		this.cache = cache;
	}

	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}

	public PointValueTime getLatestPointValue(PointValueDao thisDao, int thisDataPointId) {
		if (maxSize == 0)
			refreshCache(1, thisDao, thisDataPointId);
		List<PointValueTime> c = cache;
		if (c.size() > 0)
			return c.get(0);
		return null;
	}

	public List<PointValueTime> getLatestPointValues(int limit, PointValueDao thisDao, int thisDataPointId) {
		if (maxSize < limit)
			refreshCache(limit, thisDao, thisDataPointId);
		List<PointValueTime> c = cache;
		if (limit == c.size())
			return c;
		if (limit > c.size())
			limit = c.size();
		return new ArrayList<PointValueTime>(c.subList(0, limit));
	}

	public void refreshCache(int size, PointValueDao thisDao, int thisDataPointId) {
		if (size > maxSize) {
			maxSize = size;
			if (size == 1) {
				PointValueTime pvt = thisDao.getLatestPointValue(thisDataPointId);
				if (pvt != null) {
					List<PointValueTime> c = new ArrayList<PointValueTime>();
					c.add(pvt);
					cache = c;
				}
			} else
				cache = thisDao.getLatestPointValues(thisDataPointId, size);
		}
	}

	public void savePointValue(PointValueTime pvt, SetPointSource source, boolean logValue, boolean async,
			PointValueDao thisDao, int thisDataPointId) {
		if (logValue) {
			if (async)
				thisDao.savePointValueAsync(thisDataPointId, pvt, source);
			else
				pvt = thisDao.savePointValueSync(thisDataPointId, pvt, source);
		}
		List<PointValueTime> c = cache;
		List<PointValueTime> newCache = new ArrayList<PointValueTime>(c.size() + 1);
		newCache.addAll(c);
		int pos = 0;
		if (newCache.size() == 0)
			newCache.add(pvt);
		else {
			while (pos < newCache.size() && newCache.get(pos).getTime() > pvt.getTime())
				pos++;
			if (pos < maxSize)
				newCache.add(pos, pvt);
		}
		while (newCache.size() > maxSize)
			newCache.remove(newCache.size() - 1);
		cache = newCache;
	}
}
