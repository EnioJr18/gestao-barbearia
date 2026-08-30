ALTER TABLE vendas ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'FINALIZADA';
ALTER TABLE vendas ADD CONSTRAINT ck_vendas_status CHECK (status IN ('FINALIZADA', 'CANCELADA'));
CREATE INDEX idx_vendas_status_data ON vendas (status, data_venda);
