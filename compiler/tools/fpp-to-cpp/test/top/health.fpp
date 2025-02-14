module Svc {
  port Ping
}
module Svc {
  passive component Health {
    sync input port pingIn: [2] Svc.Ping
    output port pingOut: [2] Svc.Ping
    match pingOut with pingIn
  }
}
module M {

  passive component C {
    sync input port pingIn: Svc.Ping
    output port pingOut: Svc.Ping
  }

  instance $health: Svc.Health base id 0x100 \
    at "HealthImpl.hpp" {

    phase Phases.instances """
    Svc::HealthImpl health(FW_OPTIONAL_NAME("health"));
    """

  }

  instance c1: C base id 0x200
  instance c2: C base id 0x300

  topology Health {
    instance $health
    instance c1
    instance c2
    health connections instance $health
  }

}
