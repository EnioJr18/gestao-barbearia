package br.com.barbeirofinanceiro.application.relatorio;

import br.com.barbeirofinanceiro.domain.movimentacao.DespesaResumoProjection;
import br.com.barbeirofinanceiro.domain.movimentacao.FormaPagamento;
import br.com.barbeirofinanceiro.domain.movimentacao.MovimentacaoRepository;
import br.com.barbeirofinanceiro.domain.venda.ItemRelatorioProjection;
import br.com.barbeirofinanceiro.domain.venda.ItemVendaRepository;
import br.com.barbeirofinanceiro.domain.venda.PagamentoResumoProjection;
import br.com.barbeirofinanceiro.domain.venda.VendaPagamentoRepository;
import br.com.barbeirofinanceiro.domain.venda.VendaRepository;
import br.com.barbeirofinanceiro.domain.venda.VendaResumoProjection;
import br.com.barbeirofinanceiro.domain.venda.ClienteRelatorioProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class RelatorioService {

    private final VendaRepository vendaRepository;
    private final VendaPagamentoRepository vendaPagamentoRepository;
    private final MovimentacaoRepository movimentacaoRepository;
    private final ItemVendaRepository itemVendaRepository;

    public RelatorioService(
            VendaRepository vendaRepository,
            VendaPagamentoRepository vendaPagamentoRepository,
            MovimentacaoRepository movimentacaoRepository,
            ItemVendaRepository itemVendaRepository
    ) {
        this.vendaRepository = vendaRepository;
        this.vendaPagamentoRepository = vendaPagamentoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.itemVendaRepository = itemVendaRepository;
    }

    @Transactional(readOnly = true)
    public RelatorioResumoResponse buscarResumo(
            LocalDate dataInicial,
            LocalDate dataFinal
    ) {
        validarPeriodo(dataInicial, dataFinal);

        VendaResumoProjection vendaResumo =
                vendaRepository.findResumoByDataVendaBetween(
                        dataInicial,
                        dataFinal
                );

        DespesaResumoProjection despesaResumo =
                movimentacaoRepository.buscarResumoDespesas(
                        dataInicial,
                        dataFinal
                );

        BigDecimal faturamento = vendaResumo.faturamento();
        BigDecimal despesas = despesaResumo.despesas();
        BigDecimal resultado = faturamento.subtract(despesas);
        long quantidadeVendas = vendaResumo.quantidadeVendas();

        return new RelatorioResumoResponse(
                faturamento,
                despesas,
                resultado,
                quantidadeVendas
        );
    }

    @Transactional(readOnly = true)
    public RelatorioPagamentoResponse buscarPagamentos(
            LocalDate dataInicial,
            LocalDate dataFinal
    ) {
        validarPeriodo(dataInicial, dataFinal);

        List<PagamentoResumoProjection> pagamentos =
                vendaPagamentoRepository.buscarResumoPorFormaPagamento(
                        dataInicial,
                        dataFinal
                );

        BigDecimal dinheiro = BigDecimal.ZERO;
        BigDecimal pix = BigDecimal.ZERO;
        BigDecimal cartaoCredito = BigDecimal.ZERO;
        BigDecimal cartaoDebito = BigDecimal.ZERO;

        for (PagamentoResumoProjection pagamento : pagamentos) {
            if (pagamento.formaPagamento() == FormaPagamento.DINHEIRO) {
                dinheiro = pagamento.valor();
            } else if (pagamento.formaPagamento() == FormaPagamento.PIX) {
                pix = pagamento.valor();
            } else if (pagamento.formaPagamento() == FormaPagamento.CARTAO_CREDITO) {
                cartaoCredito = pagamento.valor();
            } else if (pagamento.formaPagamento() == FormaPagamento.CARTAO_DEBITO) {
                cartaoDebito = pagamento.valor();
            }
        }

        return new RelatorioPagamentoResponse(
                dinheiro,
                pix,
                cartaoCredito,
                cartaoDebito
        );
    }

    @Transactional(readOnly = true)
    public List<ItemRelatorioResponse> buscarItens(
            LocalDate dataInicial,
            LocalDate dataFinal
    ) {
        validarPeriodo(dataInicial, dataFinal);

        List<ItemRelatorioProjection> itens =
                itemVendaRepository.buscarResumoItens(
                        dataInicial,
                        dataFinal
                );

        return itens.stream()
                .map(item -> new ItemRelatorioResponse(
                        item.itemId(),
                        item.nome(),
                        item.tipo(),
                        item.quantidade(),
                        item.faturamento()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClienteRelatorioResponse> buscarClientes(
            LocalDate dataInicial,
            LocalDate dataFinal
    ) {
        validarPeriodo(dataInicial, dataFinal);
        return vendaRepository.buscarRankingClientes(dataInicial, dataFinal).stream()
                .map(cliente -> new ClienteRelatorioResponse(
                        cliente.clienteId(), cliente.nome(), cliente.quantidadeVendas(), cliente.valorTotal()))
                .toList();
    }

    private void validarPeriodo(
            LocalDate dataInicial,
            LocalDate dataFinal
    ) {
        if (dataInicial.isAfter(dataFinal)) {
            throw new RelatorioValidationException(
                    "A data inicial não pode ser posterior à data final"
            );
        }
    }
}
