package io.github.jmarcos.patinhodancador;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * Trava de criança: "Fixar tela" (screen pinning) do Android.
 *
 * Como o app NÃO é device owner, startLockTask() equivale ao pinning manual:
 * bloqueia Home/Recentes e o pai sai pelo gesto de desafixar do sistema
 * (segurar Voltar + Recentes). É o "máximo possível" sem provisionar o
 * aparelho como dispositivo dedicado. Chamado pelo cadeado no index.html.
 */
@CapacitorPlugin(name = "ScreenPin")
public class ScreenPinPlugin extends Plugin {

    @PluginMethod
    public void pin(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            try {
                getActivity().startLockTask();
            } catch (Exception e) {
                // Alguns aparelhos/estados não permitem; o cadeado (Voltar) ainda vale.
            }
        });
        call.resolve();
    }

    @PluginMethod
    public void unpin(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            try {
                getActivity().stopLockTask();
            } catch (Exception e) {
                // Já estava fora do modo fixado — ignora.
            }
        });
        call.resolve();
    }
}
