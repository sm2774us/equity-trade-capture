package com.balyasny.tradecapture.repository;

import com.balyasny.tradecapture.domain.Trade;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TradeRepository extends JpaRepository<Trade, String> {

    List<Trade> findByBookIdOrderByExecutionTimestampDesc(String bookId);

    @Query("select t from Trade t where t.instrumentId = ?1 order by t.executionTimestamp desc")
    List<Trade> findByInstrument(String instrumentId);
}
