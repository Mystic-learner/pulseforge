// Forge Console design: make unavailable telemetry explicit instead of visually implying zero or placeholder data.
import type { ReactNode } from "react";
export default function NoDataPanel({message="No data available",detail}:{message?:string;detail?:ReactNode}){return <div className="no-data" role="status">{message}{detail&&<span>{detail}</span>}</div>}
