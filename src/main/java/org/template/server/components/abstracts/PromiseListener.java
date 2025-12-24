package org.template.server.components.abstracts;

import org.template.server.components.pojo.Future;

public interface PromiseListener {
    public void onStateChange(Future future);
}
