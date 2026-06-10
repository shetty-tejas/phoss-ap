/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phoss.ap.otel;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.telemetry.ITelemetrySpan;
import com.helger.telemetry.TelemetryAttributes;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;

/**
 * {@link ITelemetrySpan} backed by an OpenTelemetry {@link Span} that has been made the current
 * span on the calling thread via {@link Span#makeCurrent()}. The returned {@link Scope} is kept
 * inside this wrapper and closed together with the span on <code>close()</code>.
 *
 * @author Philip Helger
 * @since 0.9.0
 */
final class OtelAPSpan implements ITelemetrySpan
{
  private final Span m_aSpan;
  private final Scope m_aScope;
  private boolean m_bClosed;

  OtelAPSpan (@NonNull final Span aSpan)
  {
    m_aSpan = aSpan;
    m_aScope = aSpan.makeCurrent ();
  }

  @NonNull
  public ITelemetrySpan setAttribute (@NonNull final String sKey, @Nullable final String sValue)
  {
    if (sValue != null)
      m_aSpan.setAttribute (sKey, sValue);
    return this;
  }

  @NonNull
  public ITelemetrySpan setAttribute (@NonNull final String sKey, final boolean bValue)
  {
    m_aSpan.setAttribute (sKey, bValue);
    return this;
  }

  @NonNull
  public ITelemetrySpan setAttribute (@NonNull final String sKey, final long nValue)
  {
    m_aSpan.setAttribute (sKey, nValue);
    return this;
  }

  @NonNull
  public ITelemetrySpan setAttribute (@NonNull final String sKey, final double dValue)
  {
    m_aSpan.setAttribute (sKey, dValue);
    return this;
  }

  @NonNull
  public ITelemetrySpan recordException (@NonNull final Throwable aException)
  {
    m_aSpan.recordException (aException);
    return this;
  }

  @NonNull
  public ITelemetrySpan addEvent (@NonNull final String sName, @NonNull final TelemetryAttributes aAttributes)
  {
    if (aAttributes.isEmpty ())
      m_aSpan.addEvent (sName);
    else
    {
      final AttributesBuilder aAttrBuilder = Attributes.builder ();
      aAttributes.forEach (new TelemetryAttributes.IVisitor ()
      {
        public void onString (@NonNull final String sKey, @NonNull final String sValue)
        {
          aAttrBuilder.put (sKey, sValue);
        }

        public void onLong (@NonNull final String sKey, final long nValue)
        {
          aAttrBuilder.put (sKey, nValue);
        }

        public void onDouble (@NonNull final String sKey, final double dValue)
        {
          aAttrBuilder.put (sKey, dValue);
        }

        public void onBoolean (@NonNull final String sKey, final boolean bValue)
        {
          aAttrBuilder.put (sKey, bValue);
        }
      });
      m_aSpan.addEvent (sName, aAttrBuilder.build ());
    }
    return this;
  }

  @NonNull
  public ITelemetrySpan setStatusOk ()
  {
    m_aSpan.setStatus (StatusCode.OK);
    return this;
  }

  @NonNull
  public ITelemetrySpan setStatusError (@Nullable final String sMessage)
  {
    if (sMessage != null)
      m_aSpan.setStatus (StatusCode.ERROR, sMessage);
    else
      m_aSpan.setStatus (StatusCode.ERROR);
    return this;
  }

  public void close ()
  {
    if (!m_bClosed)
    {
      m_bClosed = true;
      // First detach the scope (so the previous current span is restored), then end the span.
      m_aScope.close ();
      m_aSpan.end ();
    }
  }
}
