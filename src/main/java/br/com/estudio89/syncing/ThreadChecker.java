package br.com.estudio89.syncing;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by luccascorrea on 11/28/14.
 */
public class ThreadChecker {
    private List<String> threadIds = new ArrayList<String>();

    /**
     * Esse método gera um novo identificador (a data atual em milisegundos)
     * e adiciona ao atributo threadIds, o qual é uma lista que contém um
     * identificador dos threads em execução.

     * @return
     */
    public String setNewThreadId() {
        String threadId = new SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(new Date());
        threadIds.add(threadId);
        return threadId;
    }

    /**
     * Verifica se o identificador de thread passado consta dentro
     * da lista de identificadores, retornando um booleano.
     *
     * @param threadId
     * @return
     */
    public boolean isValidThreadId(String threadId) {
        return this.threadIds.contains(threadId);
    }

    /**
     * Remove um id da lista de identificadores e deve ser chamado ao final
     * da execução de cada método de sincronização.
     *
     * @param threadId
     */
    public void removeThreadId(String threadId) {
        this.threadIds.remove(threadId);
    }

    public void clear(){ this.threadIds.clear(); }
}
