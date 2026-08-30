package br.com.barbeirofinanceiro.persistence;

import br.com.barbeirofinanceiro.domain.caixa.Caixa;
import br.com.barbeirofinanceiro.domain.caixa.CaixaRepository;
import br.com.barbeirofinanceiro.domain.caixa.StatusCaixa;
import br.com.barbeirofinanceiro.domain.item.Item;
import br.com.barbeirofinanceiro.domain.item.ItemRepository;
import br.com.barbeirofinanceiro.domain.item.TipoItem;
import br.com.barbeirofinanceiro.domain.usuario.Usuario;
import br.com.barbeirofinanceiro.domain.usuario.UsuarioRepository;
import br.com.barbeirofinanceiro.domain.venda.ItemVenda;
import br.com.barbeirofinanceiro.domain.venda.ItemVendaRepository;
import br.com.barbeirofinanceiro.domain.venda.StatusVenda;
import br.com.barbeirofinanceiro.domain.venda.Venda;
import br.com.barbeirofinanceiro.domain.venda.VendaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ItemRelatorioControllerIntegrationTest extends PostgresPersistenceTest {

    private static final String USER = "item-relatorio-user@teste.local";

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CaixaRepository caixaRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private VendaRepository vendaRepository;
    @Autowired private ItemVendaRepository itemVendaRepository;

    private Caixa caixa;

    @BeforeEach
    void preparar() {
        Usuario usuario = newEntity(Usuario.class);
        usuario.setNome("Usuário Itens Relatório");
        usuario.setEmail(USER);
        usuario.setSenhaHash("hash");
        usuarioRepository.saveAndFlush(usuario);

        caixa = new Caixa();
        caixa.setDataCaixa(LocalDate.of(2026, 8, 23));
        caixa.setValorInicial(BigDecimal.ZERO);
        caixa.setStatus(StatusCaixa.ABERTO);
        caixa.setUsuarioAbertura(usuario);
        caixaRepository.saveAndFlush(caixa);
    }

    @AfterEach
    void limpar() {
        itemVendaRepository.deleteAll();
        vendaRepository.deleteAll();
        caixaRepository.deleteAll();
        itemRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void deveAgruparItensEOrdenarPorFaturamento() throws Exception {
        Item corte = item("Corte", TipoItem.SERVICO, BigDecimal.valueOf(30));
        Item barba = item("Barba", TipoItem.SERVICO, BigDecimal.valueOf(20));
        venda(LocalDate.of(2026, 8, 24), StatusVenda.FINALIZADA, corte, 2, 60);
        venda(LocalDate.of(2026, 8, 24), StatusVenda.FINALIZADA, barba, 1, 20);
        venda(LocalDate.of(2026, 8, 25), StatusVenda.FINALIZADA, corte, 1, 30);

        mockMvc.perform(get("/api/v1/relatorios/itens")
                        .param("dataInicial", "2026-08-23")
                        .param("dataFinal", "2026-08-25")
                        .with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Corte"))
                .andExpect(jsonPath("$[0].quantidade").value(3))
                .andExpect(jsonPath("$[0].faturamento").value(90))
                .andExpect(jsonPath("$[1].nome").value("Barba"))
                .andExpect(jsonPath("$[1].quantidade").value(1));
    }

    @Test
    void deveIgnorarVendaCancelada() throws Exception {
        Item corte = item("Corte cancelado", TipoItem.SERVICO, BigDecimal.valueOf(30));
        venda(LocalDate.of(2026, 8, 24), StatusVenda.CANCELADA, corte, 2, 60);

        mockMvc.perform(get("/api/v1/relatorios/itens")
                        .param("dataInicial", "2026-08-23")
                        .param("dataFinal", "2026-08-25")
                        .with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.empty()));
    }

    @Test
    void deveRespeitarPeriodo() throws Exception {
        Item corte = item("Corte fora do período", TipoItem.SERVICO, BigDecimal.valueOf(30));
        venda(LocalDate.of(2026, 8, 22), StatusVenda.FINALIZADA, corte, 1, 30);

        mockMvc.perform(get("/api/v1/relatorios/itens")
                        .param("dataInicial", "2026-08-23")
                        .param("dataFinal", "2026-08-25")
                        .with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.empty()));
    }

    @Test
    void deveRejeitarPeriodoInvalido() throws Exception {
        mockMvc.perform(get("/api/v1/relatorios/itens")
                        .param("dataInicial", "2026-08-25")
                        .param("dataFinal", "2026-08-23")
                        .with(user(USER)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("A data inicial não pode ser posterior à data final"));
    }

    private Item item(String nome, TipoItem tipo, BigDecimal preco) {
        Item item = new Item();
        item.setNome(nome);
        item.setTipo(tipo);
        item.setPreco(preco);
        item.setEstoque(null);
        return itemRepository.saveAndFlush(item);
    }

    private void venda(LocalDate data, StatusVenda status, Item item, int quantidade, int subtotal) {
        Venda venda = newEntity(Venda.class);
        venda.setCaixa(caixa);
        venda.setDataVenda(data);
        venda.setStatus(status);
        venda.setValorTotal(BigDecimal.valueOf(subtotal));
        venda = vendaRepository.saveAndFlush(venda);

        ItemVenda itemVenda = new ItemVenda();
        itemVenda.setVenda(venda);
        itemVenda.setItem(item);
        itemVenda.setQuantidade(quantidade);
        itemVenda.setPrecoUnitario(BigDecimal.valueOf(subtotal).divide(BigDecimal.valueOf(quantidade)));
        itemVenda.setSubtotal(BigDecimal.valueOf(subtotal));
        itemVendaRepository.saveAndFlush(itemVenda);
    }
}
