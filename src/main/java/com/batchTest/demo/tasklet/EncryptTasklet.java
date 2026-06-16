package com.batchTest.demo.tasklet;

import com.batchTest.demo.domain.EncTarget;
import com.batchTest.demo.service.EncryptService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class EncryptTasklet implements Tasklet {

    private final JdbcTemplate jdbcTemplate;
    private final EncryptService encryptService;

    private static final int CHUNK_SIZE = 1000;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        List<EncTarget> targets = jdbcTemplate.query(
                "SELECT * FROM ENC_TARGET WHERE PROCESSED_YN = 'N'",
                new BeanPropertyRowMapper<>(EncTarget.class)
        );

        for (EncTarget target : targets) {
            processTarget(target);
        }

        return RepeatStatus.FINISHED;
    }

    private void processTarget(EncTarget target) {

        String table = target.getTableName();
        String column = target.getColumnName();
        String pk = target.getPkColumn();

        long lastPk = target.getLastPk() == null ? 0 : target.getLastPk();

        while (true) {

            String selectSql = String.format(
                "SELECT %s, %s FROM %s " +
                "WHERE %s > ? AND (%s NOT LIKE 'ENC(%%)' OR %s IS NULL) " +
                "ORDER BY %s LIMIT %d",
                pk, column, table,
                pk, column, column,
                pk, CHUNK_SIZE
            );

            List<Map<String, Object>> rows =
                    jdbcTemplate.queryForList(selectSql, lastPk);

            if (rows.isEmpty()) {
                markComplete(target.getId());
                break;
            }

            List<Object[]> batchParams = new ArrayList<>();

            for (Map<String, Object> row : rows) {

                Long pkVal = ((Number) row.get(pk)).longValue();
                Object value = row.get(column);

                if (value != null) {
                    String enc = encryptService.encrypt(value.toString());
                    batchParams.add(new Object[]{enc, pkVal});
                }

                lastPk = pkVal;
            }

            String updateSql = String.format(
                "UPDATE %s SET %s = ? WHERE %s = ?",
                table, column, pk
            );

            jdbcTemplate.batchUpdate(updateSql, batchParams);

            updateLastPk(target.getId(), lastPk);
        }
    }

    private void updateLastPk(Long id, Long lastPk) {
        jdbcTemplate.update(
                "UPDATE ENC_TARGET SET LAST_PK = ?, UPDATED_AT = NOW() WHERE ID = ?",
                lastPk, id
        );
    }

    private void markComplete(Long id) {
        jdbcTemplate.update(
                "UPDATE ENC_TARGET SET PROCESSED_YN = 'Y', UPDATED_AT = NOW() WHERE ID = ?",
                id
        );
    }
}